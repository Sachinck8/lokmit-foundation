package com.lokmit.foundation.employment.resume.service.storage;

import com.lokmit.foundation.employment.resume.entity.ResumeFileBlob;
import com.lokmit.foundation.employment.resume.repository.ResumeFileBlobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the A7.6.1 database storage implementation: key
 * mapping, store/load round-trip, idempotent delete and malformed-key
 * rejection (a silent ignore would hide caller bugs).
 */
class DatabaseFileStorageTest {

    private ResumeFileBlobRepository blobRepository;
    private DatabaseFileStorage storage;

    @BeforeEach
    void setUp() {
        blobRepository = mock(ResumeFileBlobRepository.class);
        storage = new DatabaseFileStorage(blobRepository);
    }

    @Test
    @DisplayName("store persists the bytes under the resume id parsed from the key")
    void storeMapsKeyToResumeId() {
        byte[] content = "pdf-bytes".getBytes();

        String returned = storage.store("resumes/42/9f1c2f3e-8f2e-4a4b-b7a3-1c9d5e6f7a8b", content);

        assertThat(returned).isEqualTo("resumes/42/9f1c2f3e-8f2e-4a4b-b7a3-1c9d5e6f7a8b");
        ArgumentCaptor<ResumeFileBlob> captor = ArgumentCaptor.forClass(ResumeFileBlob.class);
        verify(blobRepository).save(captor.capture());
        assertThat(captor.getValue().getResumeId()).isEqualTo(42L);
        assertThat(captor.getValue().getContent()).isEqualTo(content);
    }

    @Test
    @DisplayName("load returns the exact stored bytes and null when absent")
    void loadRoundTrip() {
        byte[] content = "resume-bytes".getBytes();
        ResumeFileBlob blob = new ResumeFileBlob();
        blob.setResumeId(7L);
        blob.setContent(content);
        when(blobRepository.findByResumeId(7L)).thenReturn(Optional.of(blob));
        when(blobRepository.findByResumeId(8L)).thenReturn(Optional.empty());

        assertThat(storage.load("resumes/7/uuid")).isEqualTo(content);
        assertThat(storage.load("resumes/8/uuid")).isNull();
    }

    @Test
    @DisplayName("delete is idempotent — absent key deletes nothing and does not fail")
    void deleteIdempotent() {
        storage.delete("resumes/9/uuid");

        verify(blobRepository).deleteById(9L);
        verify(blobRepository, never()).save(any());
    }

    @Test
    @DisplayName("malformed or foreign-shaped keys are rejected, never silently ignored")
    void malformedKeysRejected() {
        assertThatThrownBy(() -> storage.store("not-a-key", new byte[1]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.store("candidates/7/uuid", new byte[1]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.load("resumes/notanumber/uuid"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.load("resumes/7"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(blobRepository, never()).save(any());
        verify(blobRepository, never()).deleteById(any());
        verify(blobRepository, never()).findByResumeId(any());
    }
}
