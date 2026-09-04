package com.lokmit.foundation.security.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void encode_shouldHashPassword() {
        String rawPassword = "SecureP@ss123";
        String encoded = passwordEncoder.encode(rawPassword);

        assertThat(encoded).isNotEqualTo(rawPassword);
        assertThat(encoded).startsWith("$2a$"); // BCrypt prefix
    }

    @Test
    void matches_shouldReturnTrueForCorrectPassword() {
        String rawPassword = "SecureP@ss123";
        String encoded = passwordEncoder.encode(rawPassword);

        assertThat(passwordEncoder.matches(rawPassword, encoded)).isTrue();
    }

    @Test
    void matches_shouldReturnFalseForIncorrectPassword() {
        String rawPassword = "SecureP@ss123";
        String wrongPassword = "WrongP@ss123";
        String encoded = passwordEncoder.encode(rawPassword);

        assertThat(passwordEncoder.matches(wrongPassword, encoded)).isFalse();
    }

    @Test
    void encode_shouldGenerateDifferentHashesForSamePassword() {
        String rawPassword = "SecureP@ss123";
        String encoded1 = passwordEncoder.encode(rawPassword);
        String encoded2 = passwordEncoder.encode(rawPassword);

        // BCrypt generates different hashes due to random salt
        assertThat(encoded1).isNotEqualTo(encoded2);
        // But both should match the original password
        assertThat(passwordEncoder.matches(rawPassword, encoded1)).isTrue();
        assertThat(passwordEncoder.matches(rawPassword, encoded2)).isTrue();
    }

    @Test
    void plaintextPassword_shouldNeverBeStored() {
        String rawPassword = "SecureP@ss123";
        String encoded = passwordEncoder.encode(rawPassword);

        assertThat(encoded).doesNotContain(rawPassword);
        assertThat(encoded).doesNotContain("SecureP@ss");
    }
}