package com.lokmit.foundation.employment.resume.service;

import com.lokmit.foundation.audit.service.AuditLogService;
import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.resume.entity.Resume;
import com.lokmit.foundation.employment.resume.repository.ResumeRepository;
import com.lokmit.foundation.employment.resume.service.storage.FileStorage;
import com.lokmit.foundation.security.Permissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * A7.6.6 item 3: resume deletion audit integration. Proves the audit row
 * is written for BOTH actor classes (candidate self-service vs
 * candidates:manage admin), that details stay metadata-only, and that a
 * failed or rejected deletion writes no audit row.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResumeDeleteServiceAuditTest {

    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private ResumeOwnershipService ownershipService;
    @Mock
    private FileStorage fileStorage;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private com.lokmit.foundation.security.util.SecurityUtils securityUtils;

    private ResumeDeleteService service;

    @Captor
    private ArgumentCaptor<Map<String, Object>> detailsCaptor;

    @BeforeEach
    void setUp() {
        service = new ResumeDeleteService(resumeRepository, ownershipService,
                fileStorage, auditLogService, securityUtils);

        when(securityUtils.hasAuthority(Permissions.CANDIDATES_MANAGE)).thenReturn(false);

        Candidate own = new Candidate();
        own.setId(50L);
        own.setAvailabilityStatus(Candidate.AVAILABILITY_ACTIVELY_LOOKING);
        own.setCreatedAt(OffsetDateTime.now());
        own.setUpdatedAt(OffsetDateTime.now());
        when(ownershipService.resolveOwnCandidate(2L)).thenReturn(own);

        when(resumeRepository.findById(10L)).thenReturn(Optional.of(resume(10L, 50L, true)));
    }

    private static Resume resume(long id, long candidateId, boolean active) {
        Resume r = new Resume();
        r.setId(id);
        r.setCandidateId(candidateId);
        r.setFileName("cv.pdf");
        r.setFileType("application/pdf");
        r.setFileSizeBytes(100L);
        r.setActive(active);
        r.setCreatedAt(OffsetDateTime.now());
        r.setChecksumSha256("a".repeat(64));
        r.setStorageKey("resumes/" + id + "/key");
        return r;
    }

    @Test
    @DisplayName("candidate deletes own active resume → RESUME_DELETED with candidateId + wasActive")
    void candidateDeleteAuditsResumeDeleted() {
        service.deleteForPrincipal(10L, 2L, false);

        verify(auditLogService).record(eq("RESUME_DELETED"), eq("RESUME"), eq(10L), detailsCaptor.capture());
        assertThat(detailsCaptor.getValue())
                .containsEntry("candidateId", 50L)
                .containsEntry("wasActive", true);
        // Metadata only — no storage key, no checksum, no bytes.
        assertThat(detailsCaptor.getValue().toString())
                .doesNotContain("resumes/10/key")
                .doesNotContain("a".repeat(64));
    }

    @Test
    @DisplayName("admin deletion → RESUME_DELETED_ADMIN (actor class distinguishable)")
    void adminDeleteAuditsResumeDeletedAdmin() {
        when(securityUtils.hasAuthority(Permissions.CANDIDATES_MANAGE)).thenReturn(true);

        service.deleteForPrincipal(10L, 4L, true);

        verify(auditLogService).record(eq("RESUME_DELETED_ADMIN"), eq("RESUME"), eq(10L), any());
        verify(securityUtils).hasAuthority(Permissions.CANDIDATES_MANAGE);
    }

    @Test
    @DisplayName("inactive resume deletion by admin → RESUME_DELETED_ADMIN with wasActive=false")
    void adminDeleteInactiveAuditsWasActiveFalse() {
        when(securityUtils.hasAuthority(Permissions.CANDIDATES_MANAGE)).thenReturn(true);
        when(resumeRepository.findById(11L)).thenReturn(Optional.of(resume(11L, 50L, false)));

        service.deleteForPrincipal(11L, 4L, true);

        verify(auditLogService).record(eq("RESUME_DELETED_ADMIN"), eq("RESUME"), eq(11L), detailsCaptor.capture());
        assertThat(detailsCaptor.getValue())
                .containsEntry("candidateId", 50L)
                .containsEntry("wasActive", false);
    }

    @Test
    @DisplayName("audit is written AFTER metadata removal + storage cleanup (same transaction path)")
    void auditRunsAfterCleanup() {
        service.deleteForPrincipal(10L, 2L, false);

        InOrder order = inOrder(resumeRepository, fileStorage, auditLogService);
        order.verify(resumeRepository).delete(any(Resume.class));
        order.verify(resumeRepository).flush();
        order.verify(fileStorage).delete("resumes/10/key");
        order.verify(auditLogService).record(eq("RESUME_DELETED"), eq("RESUME"), eq(10L), any());
    }

    @Test
    @DisplayName("foreign resume → 404 BEFORE any audit interaction")
    void foreignResumeAuditsNothing() {
        when(resumeRepository.findById(12L)).thenReturn(Optional.of(resume(12L, 99L, true)));

        assertThatThrownBy(() -> service.deleteForPrincipal(12L, 2L, false))
                .isInstanceOf(com.lokmit.foundation.common.exception.NotFoundException.class);

        verifyNoInteractions(auditLogService);
        verify(resumeRepository, never()).delete(any(Resume.class));
    }

    @Test
    @DisplayName("nonexistent resume → 404 BEFORE any audit interaction")
    void nonexistentResumeAuditsNothing() {
        when(resumeRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteForPrincipal(404L, 2L, false))
                .isInstanceOf(com.lokmit.foundation.common.exception.NotFoundException.class);

        verifyNoInteractions(auditLogService);
    }

    @Test
    @DisplayName("storage failure → audit NOT reached (whole operation fails atomically)")
    void storageFailurePreventsAudit() {
        doThrow(new RuntimeException("storage down")).when(fileStorage).delete(anyString());

        assertThatThrownBy(() -> service.deleteForPrincipal(10L, 2L, false))
                .isInstanceOf(RuntimeException.class);

        verify(auditLogService, never()).record(anyString(), anyString(), anyLong(), any());
    }
}
