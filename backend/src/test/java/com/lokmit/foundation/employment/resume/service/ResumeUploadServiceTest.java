package com.lokmit.foundation.employment.resume.service;

import com.lokmit.foundation.common.exception.BadRequestException;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.common.exception.PayloadTooLargeException;
import com.lokmit.foundation.employment.candidate.repository.CandidateRepository;
import com.lokmit.foundation.employment.resume.config.UploadProperties;
import com.lokmit.foundation.employment.resume.entity.Resume;
import com.lokmit.foundation.employment.resume.repository.ResumeRepository;
import com.lokmit.foundation.employment.resume.service.storage.FileStorage;
import com.lokmit.foundation.employment.resume.service.validation.ResumeContentValidator;
import com.lokmit.foundation.employment.resume.service.validation.ResumeFilenameValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the A7.6.2 upload lifecycle: validation ordering,
 * server-generated storage key, checksum recording, exact-byte storage,
 * failure atomicity and the re-upload deactivation ordering against the
 * partial unique index. Pure Mockito — no Spring context, no database.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResumeUploadServiceTest {

    private static final String WORD_CT =
            "<?xml version=\"1.0\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                    + "<Default Extension=\"xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
                    + "</Types>";

    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private CandidateRepository candidateRepository;
    @Mock
    private FileStorage fileStorage;

    private ResumeUploadService service;

    @BeforeEach
    void setUp() {
        ResumeContentValidator contentValidator = new ResumeContentValidator();
        ResumeFilenameValidator filenameValidator = new ResumeFilenameValidator();
        UploadProperties uploadProperties = new UploadProperties();
        service = new ResumeUploadService(contentValidator, filenameValidator,
                resumeRepository, candidateRepository, fileStorage, uploadProperties);

        when(candidateRepository.existsById(10L)).thenReturn(true);
        when(resumeRepository.saveAndFlush(any(Resume.class))).thenAnswer(inv -> {
            Resume r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(77L); // Simulate identity assignment on flush.
            }
            return r;
        });
        when(resumeRepository.deactivateActiveResume(10L)).thenReturn(0);
    }

    private static byte[] validDocx() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ZipOutputStream zip = new ZipOutputStream(bytes);
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write(WORD_CT.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write("<w:document/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.close();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] validPdf() {
        return "%PDF-1.4\n%fake-body\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    }

    // ------------------------------------------------------------------
    // Happy path
    // ------------------------------------------------------------------

    @Test
    @DisplayName("successful upload validates, stores exact bytes, records key + checksum")
    void successfulUploadStoresEverything() {
        byte[] content = validDocx();

        Resume result = service.upload(10L, "resume.pdf.docx", "application/octet-stream", content);

        assertThat(result.getCandidateId()).isEqualTo(10L);
        assertThat(result.getFileName()).isEqualTo("resume.pdf.docx");
        assertThat(result.getFileSizeBytes()).isEqualTo((long) content.length);
        assertThat(result.isActive()).isTrue();
        // Server-generated opaque storage key: resumes/{id}/{uuid}
        assertThat(result.getStorageKey())
                .matches(Pattern.compile("^resumes/77/[0-9a-f-]{36}$"));
        // Legacy NOT NULL column carries the non-dereferenceable marker only.
        assertThat(result.getFileUrl()).isEqualTo(ResumeUploadService.LEGACY_FILE_URL_MARKER);
        // file_type comes from CONTENT detection, not the client claim.
        assertThat(result.getFileType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        // Checksum is the deterministic SHA-256 of the exact validated bytes.
        assertThat(result.getChecksumSha256()).hasSize(64);
        assertThat(result.getChecksumSha256())
                .isEqualTo(ResumeUploadService.sha256Hex(content));

        // Exact validated bytes stored under the generated key.
        verify(fileStorage).store(eq(result.getStorageKey()), eq(content));

        // No public URL, no blob exposure through metadata.
        assertThat(result.getFileUrl()).doesNotContain("http");
    }

    @Test
    @DisplayName("lifecycle order: deactivate BEFORE insert, metadata flush BEFORE storage, "
            + "storage BEFORE checksum recording")
    void lifecycleOrderingIsExplicit() {
        // The captor would only hold a reference to the same mutable entity,
        // so snapshot the key/checksum state AT INVOCATION time instead.
        java.util.List<String> storageKeyAtSave = new java.util.ArrayList<>();
        java.util.List<String> checksumAtSave = new java.util.ArrayList<>();
        org.mockito.stubbing.Answer<Resume> snapshot = inv -> {
            Resume r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(77L);
            }
            storageKeyAtSave.add(r.getStorageKey());
            checksumAtSave.add(r.getChecksumSha256());
            return r;
        };
        org.mockito.Mockito.doAnswer(snapshot)
                .when(resumeRepository).saveAndFlush(any(Resume.class));

        service.upload(10L, "cv.pdf", "application/pdf", validPdf());

        // One consuming chain covering the full lifecycle: deactivate →
        // metadata insert (id assigned) → bytes stored → key+checksum recorded.
        InOrder lifecycle = inOrder(resumeRepository, fileStorage);
        lifecycle.verify(resumeRepository).deactivateActiveResume(10L);
        lifecycle.verify(resumeRepository).saveAndFlush(any(Resume.class));
        lifecycle.verify(fileStorage).store(anyString(), any(byte[].class));
        lifecycle.verify(resumeRepository).saveAndFlush(any(Resume.class));

        // First save carries no key/checksum yet; the second records both.
        org.mockito.Mockito.verify(resumeRepository, times(2)).saveAndFlush(any(Resume.class));
        assertThat(storageKeyAtSave).hasSize(2);
        assertThat(storageKeyAtSave.get(0)).isNull();
        assertThat(checksumAtSave.get(0)).isNull();
        assertThat(storageKeyAtSave.get(1)).isNotNull();
        assertThat(checksumAtSave.get(1)).isNotNull();
    }

    // ------------------------------------------------------------------
    // Re-upload / unique active constraint
    // ------------------------------------------------------------------

    @Test
    @DisplayName("re-upload deactivates the previous active resume before inserting")
    void reUploadDeactivatesPreviousActive() {
        when(resumeRepository.deactivateActiveResume(10L)).thenReturn(1);

        service.upload(10L, "new.pdf", "application/pdf", validPdf());

        InOrder lifecycle = inOrder(resumeRepository, fileStorage);
        lifecycle.verify(resumeRepository).deactivateActiveResume(10L);
        lifecycle.verify(resumeRepository).saveAndFlush(any(Resume.class));
        lifecycle.verify(fileStorage).store(anyString(), any(byte[].class));
        lifecycle.verify(resumeRepository).saveAndFlush(any(Resume.class));
    }

    // ------------------------------------------------------------------
    // Validation failures persist nothing
    // ------------------------------------------------------------------

    @Test
    @DisplayName("oversized upload fails fast and persists nothing")
    void oversizedUploadPersistsNothing() {
        byte[] big = new byte[(int) (5L * 1024 * 1024 + 1)];
        big[0] = '%';
        big[1] = 'P';

        assertThatThrownBy(() -> service.upload(10L, "big.pdf", "application/pdf", big))
                .isInstanceOf(PayloadTooLargeException.class);

        verify(resumeRepository, never()).saveAndFlush(any(Resume.class));
        verify(fileStorage, never()).store(anyString(), any(byte[].class));
        verify(resumeRepository, never()).deactivateActiveResume(any());
    }

    @Test
    @DisplayName("failed content validation persists nothing")
    void failedValidationPersistsNothing() {
        assertThatThrownBy(() -> service.upload(10L, "evil.pdf", "application/pdf",
                "not a resume".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(BadRequestException.class);

        verify(resumeRepository, never()).saveAndFlush(any(Resume.class));
        verify(fileStorage, never()).store(anyString(), any(byte[].class));
        verify(resumeRepository, never()).deactivateActiveResume(any());
    }

    @Test
    @DisplayName("failed filename validation persists nothing")
    void failedFilenameValidationPersistsNothing() {
        assertThatThrownBy(() -> service.upload(10L, "../evil.pdf", "application/pdf", validPdf()))
                .isInstanceOf(BadRequestException.class);

        verify(resumeRepository, never()).saveAndFlush(any(Resume.class));
        verify(fileStorage, never()).store(anyString(), any(byte[].class));
        verify(resumeRepository, never()).deactivateActiveResume(any());
    }

    @Test
    @DisplayName("unknown candidate is a clean NotFound and persists nothing")
    void unknownCandidateRejected() {
        when(candidateRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.upload(99L, "cv.pdf", "application/pdf", validPdf()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Candidate not found");

        verify(resumeRepository, never()).saveAndFlush(any(Resume.class));
        verify(fileStorage, never()).store(anyString(), any(byte[].class));
        // Deactivation must not run for a non-existent candidate.
        verify(resumeRepository, never()).deactivateActiveResume(any());
    }

    @Test
    @DisplayName("null candidate id is rejected before any persistence")
    void nullCandidateRejected() {
        assertThatThrownBy(() -> service.upload(null, "cv.pdf", "application/pdf", validPdf()))
                .isInstanceOf(BadRequestException.class);

        verify(resumeRepository, never()).saveAndFlush(any(Resume.class));
        verify(fileStorage, never()).store(anyString(), any(byte[].class));
    }

    @Test
    @DisplayName("empty content is rejected before any persistence")
    void emptyContentRejected() {
        assertThatThrownBy(() -> service.upload(10L, "cv.pdf", "application/pdf", new byte[0]))
                .isInstanceOf(BadRequestException.class);

        verify(resumeRepository, never()).saveAndFlush(any(Resume.class));
        verify(fileStorage, never()).store(anyString(), any(byte[].class));
    }

    // ------------------------------------------------------------------
    // Storage failure → metadata rollback (transaction semantics)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("storage failure propagates before checksum recording — "
            + "the surrounding transaction rolls back metadata + bytes (atomic by propagation)")
    void storageFailureRollsBackMetadata() {
        byte[] content = validPdf();
        org.springframework.dao.DataAccessResourceFailureException failure =
                new org.springframework.dao.DataAccessResourceFailureException("storage down");
        doThrow(failure).when(fileStorage).store(anyString(), any(byte[].class));

        assertThatThrownBy(() -> service.upload(10L, "cv.pdf", "application/pdf", content))
                .isSameAs(failure);

        // The metadata insert WAS attempted (it shares the transaction and is
        // rolled back by the @Transactional boundary — verified in integration
        // tests); the checksum-recording save NEVER ran.
        verify(resumeRepository, times(1)).saveAndFlush(any(Resume.class));
        verify(fileStorage).store(anyString(), any(byte[].class));
    }

    // ------------------------------------------------------------------
    // Checksum utility
    // ------------------------------------------------------------------

    @Test
    @DisplayName("SHA-256 utility is deterministic and standard (empty-input vector)")
    void sha256Deterministic() {
        String expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        assertThat(ResumeUploadService.sha256Hex(new byte[0])).isEqualTo(expected);
        assertThat(ResumeUploadService.sha256Hex(new byte[0]))
                .isEqualTo(ResumeUploadService.sha256Hex(new byte[0]));
    }
}
