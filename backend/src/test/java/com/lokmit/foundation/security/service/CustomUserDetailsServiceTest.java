package com.lokmit.foundation.security.service;

import com.lokmit.foundation.security.entity.Permission;
import com.lokmit.foundation.security.entity.Role;
import com.lokmit.foundation.security.entity.User;
import com.lokmit.foundation.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the database-backed principal mapping (A1).
 *
 * <p>Key invariant: the account is {@code enabled} only when
 * {@code status == ACTIVE} — fail-closed for LOCKED, SUSPENDED, DELETED, and
 * any unexpected status value, so the JWT authentication filter rejects
 * deactivated accounts even with a valid, unexpired access token.</p>
 */
class CustomUserDetailsServiceTest {

    private UserRepository userRepository;
    private CustomUserDetailsService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new CustomUserDetailsService(userRepository);
    }

    private User user(String status) {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@lokmitfoundation.org");
        user.setPasswordHash("$2a$10$examplehashexamplehashexamplehashexamplehashexamplehash");
        user.setFullName("Test User");
        user.setUserType("STAFF");
        user.setStatus(status);
        user.setEmailVerified(true);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());

        Role role = new Role();
        role.setCode("ADMIN");
        Permission permission = new Permission();
        permission.setCode("messages:manage");
        role.setPermissions(Set.of(permission));
        user.setRoles(Set.of(role));
        return user;
    }

    @Test
    @DisplayName("ACTIVE account is enabled with ROLE_ + permission authorities")
    void activeAccountIsEnabledWithAuthorities() {
        when(userRepository.findByEmail("user@lokmitfoundation.org"))
                .thenReturn(Optional.of(user("ACTIVE")));

        UserDetails details = service.loadUserByUsername("user@lokmitfoundation.org");

        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        Set<String> authorities = details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        assertThat(authorities).containsExactlyInAnyOrder("ROLE_ADMIN", "messages:manage");
        // The principal exposes the email as username; never the password hash raw form.
        assertThat(details.getUsername()).isEqualTo("user@lokmitfoundation.org");
        assertThat(details.getPassword()).startsWith("$2a$");
    }

    @ParameterizedTest(name = "status {0} produces a disabled principal")
    @ValueSource(strings = {"LOCKED", "SUSPENDED", "DELETED", "SOMETHING_ELSE"})
    @DisplayName("Non-ACTIVE statuses are disabled (fail closed)")
    void nonActiveStatusesAreDisabled(String status) {
        when(userRepository.findByEmail("user@lokmitfoundation.org"))
                .thenReturn(Optional.of(user(status)));

        UserDetails details = service.loadUserByUsername("user@lokmitfoundation.org");

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Unknown email raises UsernameNotFoundException")
    void unknownUserThrows() {
        when(userRepository.findByEmail("ghost@lokmitfoundation.org")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost@lokmitfoundation.org"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("NULL password hash maps to an empty credential without failing")
    void nullPasswordHashIsHandled() {
        User user = user("ACTIVE");
        user.setPasswordHash(null);
        when(userRepository.findByEmail("user@lokmitfoundation.org")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("user@lokmitfoundation.org");

        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getPassword()).isEmpty();
    }
}
