package com.lokmit.foundation.employment.resume.service.validation;

import com.lokmit.foundation.common.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the A7.6.2 filename security validator: traversal,
 * separators, drive paths, control/illegal characters and the
 * VARCHAR(255) length bound are all rejected WITHOUT silent rewriting.
 */
class ResumeFilenameValidatorTest {

    private final ResumeFilenameValidator validator = new ResumeFilenameValidator();

    @Test
    @DisplayName("normal valid filename passes through (normalized whitespace)")
    void normalFilenameAccepted() {
        assertThat(validator.validate("john_doe_resume.pdf")).isEqualTo("john_doe_resume.pdf");
        assertThat(validator.validate("  My   Resume.pdf  ")).isEqualTo("My Resume.pdf");
    }

    @Test
    @DisplayName("../ traversal is rejected")
    void dotDotSlashRejected() {
        assertThatThrownBy(() -> validator.validate("../../etc/passwd"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("traversal");
    }

    @Test
    @DisplayName("embedded .. without separator is rejected too")
    void embeddedDotDotRejected() {
        assertThatThrownBy(() -> validator.validate("re..sume.pdf"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("traversal");
    }

    @Test
    @DisplayName("absolute Unix path is rejected")
    void absoluteUnixPathRejected() {
        assertThatThrownBy(() -> validator.validate("/etc/passwd"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("path separators");
    }

    @Test
    @DisplayName("Windows drive-qualified path is rejected")
    void windowsDrivePathRejected() {
        assertThatThrownBy(() -> validator.validate("C:\\Users\\victim\\resume.pdf"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> validator.validate("C:/Users/resume.pdf"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("slash and backslash are rejected anywhere")
    void separatorsRejected() {
        assertThatThrownBy(() -> validator.validate("dir/resume.pdf"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> validator.validate("dir\\resume.pdf"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("control characters are rejected")
    void controlCharactersRejected() {
        assertThatThrownBy(() -> validator.validate("resume\u0000.pdf"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("control");
        assertThatThrownBy(() -> validator.validate("resume\n.pdf"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> validator.validate("resume\u007F.pdf"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Windows-illegal characters are rejected")
    void illegalCharactersRejected() {
        for (char c : "<>:\"|?*".toCharArray()) {
            assertThatThrownBy(() -> validator.validate("resume" + c + ".pdf"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("illegal");
        }
    }

    @Test
    @DisplayName("a filename over 255 characters is rejected")
    void overlyLongFilenameRejected() {
        String longName = "a".repeat(256) + ".pdf";
        assertThatThrownBy(() -> validator.validate(longName))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("255");
    }

    @Test
    @DisplayName("exactly 255 characters is accepted (VARCHAR(255) boundary)")
    void exactly255Accepted() {
        String name = "a".repeat(251) + ".pdf";
        assertThat(name).hasSize(255);
        assertThatCode(() -> validator.validate(name)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("null, blank, dot and dotdot names are rejected")
    void emptyAndDotNamesRejected() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> validator.validate("   "))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> validator.validate("."))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> validator.validate(".."))
                .isInstanceOf(BadRequestException.class);
    }
}
