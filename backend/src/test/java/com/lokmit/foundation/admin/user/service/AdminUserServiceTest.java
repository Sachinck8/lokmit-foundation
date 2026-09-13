package com.lokmit.foundation.admin.user.service;

import com.lokmit.foundation.admin.user.dto.UpdateUserRolesRequest;
import com.lokmit.foundation.admin.user.dto.UpdateUserStatusRequest;
import com.lokmit.foundation.common.exception.BadRequestException;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.security.entity.Role;
import com.lokmit.foundation.security.entity.User;
import com.lokmit.foundation.security.repository.RefreshTokenRepository;
import com.lokmit.foundation.security.repository.RoleRepository;
import com.lokmit.foundation.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the A3 protection rules: self-protection, last-SUPER_ADMIN
 * protection, SUPER_ADMIN-grant authorization, refresh-token revocation on
 * deactivation, search/filter/sort behavior and DTO safety.
 */
@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private AdminUserService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(userRepository, roleRepository, refreshTokenRepository);
        // Echo the persisted entity back, like JPA would.
        org.mockito.Mockito.lenient().when(userRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private User user(long id, String email, String status, Role... roles) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash("$2a$10$hashnotusedhere0123456789012345678901234567890");
        user.setFullName("Test User");
        user.setUserType("STAFF");
        user.setStatus(status);
        user.setEmailVerified(true);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        user.setRoles(new java.util.HashSet<>(Set.of(roles)));
        return user;
    }

    private Role role(String code, String... permissionCodes) {
        Role role = new Role();
        role.setCode(code);
        role.setPermissions(new java.util.HashSet<>());
        for (String code0 : permissionCodes) {
            com.lokmit.foundation.security.entity.Permission permission =
                    new com.lokmit.foundation.security.entity.Permission();
            permission.setCode(code0);
            role.getPermissions().add(permission);
        }
        return role;
    }

    private com.lokmit.foundation.admin.user.dto.AdminUserResponse sampleResponse(long id) {
        return com.lokmit.foundation.admin.user.dto.AdminUserResponse.builder()
                .id(id).email("u" + id + "@example.com").status("ACTIVE").build();
    }

    // ------------------------------------------------------------------
    // listUsers: search/filter/sort behavior
    // ------------------------------------------------------------------

    @Test
    @DisplayName("listUsers passes a composed Specification and maps DTOs only")
    void listUsersUsesSpecifications() {
        Pageable pageable = PageRequest.of(0, 20);
        User row = user(2L, "u2@example.com", "ACTIVE", role("MODERATOR"));
        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(row), pageable, 1));

        Page<com.lokmit.foundation.admin.user.dto.AdminUserResponse> page =
                service.listUsers("ram", "ACTIVE", "MODERATOR", pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getEmail()).isEqualTo("u2@example.com");
        assertThat(page.getContent().get(0).getRoles()).containsExactly("MODERATOR");
    }

    // ------------------------------------------------------------------
    // updateStatus: protections and side effects
    // ------------------------------------------------------------------

    @Test
    @DisplayName("status change of another user succeeds and revokes refresh tokens on deactivation")
    void statusChangeRevokesRefreshTokens() {
        User target = user(5L, "target@example.com", "ACTIVE", role("MODERATOR"));
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));

        var request = new UpdateUserStatusRequest();
        request.setStatus("SUSPENDED");
        var response = service.updateStatus(5L, request, 1L, true);

        assertThat(response.getStatus()).isEqualTo("SUSPENDED");
        verify(refreshTokenRepository).revokeAllByUserId(eq(5L), any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("re-activation does not revoke refresh tokens")
    void reactivationDoesNotRevoke() {
        User target = user(5L, "target@example.com", "SUSPENDED", role("MODERATOR"));
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));

        var request = new UpdateUserStatusRequest();
        request.setStatus("ACTIVE");
        service.updateStatus(5L, request, 1L, true);

        verify(refreshTokenRepository, org.mockito.Mockito.never())
                .revokeAllByUserId(any(), any());
    }

    @Test
    @DisplayName("an administrator cannot demote their own account to non-ACTIVE (400)")
    void selfDemotionIsRejected() {
        var request = new UpdateUserStatusRequest();
        request.setStatus("SUSPENDED");

        assertThatThrownBy(() -> service.updateStatus(1L, request, 1L, true))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("own account");
    }

    @Test
    @DisplayName("same status on self is allowed (no-op style update)")
    void sameStatusOnSelfIsAllowed() {
        User target = user(1L, "admin@example.com", "ACTIVE", role("SUPER_ADMIN"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(target));

        var request = new UpdateUserStatusRequest();
        request.setStatus("ACTIVE");
        var response = service.updateStatus(1L, request, 1L, true);

        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("disabling the last active SUPER_ADMIN is rejected (400)")
    void lastSuperAdminCannotBeDisabled() {
        User target = user(1L, "admin@example.com", "ACTIVE", role("SUPER_ADMIN"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(target));
        when(userRepository.count(any(Specification.class))).thenReturn(1L);

        var request = new UpdateUserStatusRequest();
        request.setStatus("LOCKED");

        assertThatThrownBy(() -> service.updateStatus(1L, request, 2L, true))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("last active SUPER_ADMIN");
    }

    @Test
    @DisplayName("disabling a SUPER_ADMIN when another active one exists is allowed")
    void nonLastSuperAdminCanBeDisabled() {
        User target = user(1L, "admin@example.com", "ACTIVE", role("SUPER_ADMIN"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(target));
        when(userRepository.count(any(Specification.class))).thenReturn(2L);

        var request = new UpdateUserStatusRequest();
        request.setStatus("SUSPENDED");
        var response = service.updateStatus(1L, request, 2L, true);

        assertThat(response.getStatus()).isEqualTo("SUSPENDED");
    }

    @Test
    @DisplayName("disabling a non-SUPER_ADMIN user does not run the last-admin count")
    void nonSuperAdminSkipCount() {
        User target = user(5L, "mod@example.com", "ACTIVE", role("MODERATOR"));
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));

        var request = new UpdateUserStatusRequest();
        request.setStatus("LOCKED");
        service.updateStatus(5L, request, 1L, true);

        verify(userRepository, org.mockito.Mockito.never())
                .count(any(Specification.class));
    }

    @Test
    @DisplayName("unknown status value is rejected with 400")
    void invalidStatusIs400() {
        var request = new UpdateUserStatusRequest();
        request.setStatus("BANNED");

        assertThatThrownBy(() -> service.updateStatus(5L, request, 1L, true))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ACTIVE, LOCKED, SUSPENDED, DELETED");
    }

    @Test
    @DisplayName("unknown target user returns 404")
    void unknownUserIs404() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        var request = new UpdateUserStatusRequest();
        request.setStatus("ACTIVE");

        assertThatThrownBy(() -> service.updateStatus(99L, request, 1L, true))
                .isInstanceOf(NotFoundException.class);
    }

    // ------------------------------------------------------------------
    // updateRoles: privilege-escalation protections
    // ------------------------------------------------------------------

    @Test
    @DisplayName("roles are replaced via the role repository and persisted")
    void rolesAreReplaced() {
        User target = user(5L, "target@example.com", "ACTIVE", role("CANDIDATE"));
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));
        Role moderator = role("MODERATOR");
        when(roleRepository.findByCode("MODERATOR")).thenReturn(Optional.of(moderator));

        var request = new UpdateUserRolesRequest();
        request.setRoles(Set.of("MODERATOR"));
        var response = service.updateRoles(5L, request, 1L, true);

        assertThat(response.getRoles()).containsExactly("MODERATOR");
        assertThat(target.getRoles()).containsExactly(moderator);
    }

    @Test
    @DisplayName("an actor cannot strip their own roles (400)")
    void selfRoleChangeIsRejected() {
        var request = new UpdateUserRolesRequest();
        request.setRoles(Set.of("MODERATOR"));

        assertThatThrownBy(() -> service.updateRoles(1L, request, 1L, true))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("own role");
    }

    @Test
    @DisplayName("removing SUPER_ADMIN from the last active SUPER_ADMIN is rejected (400)")
    void lastSuperAdminCannotBeStripped() {
        User target = user(1L, "admin@example.com", "ACTIVE", role("SUPER_ADMIN"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(target));
        when(userRepository.count(any(Specification.class))).thenReturn(1L);

        var request = new UpdateUserRolesRequest();
        request.setRoles(Set.of("MODERATOR"));

        assertThatThrownBy(() -> service.updateRoles(1L, request, 2L, true))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("last active SUPER_ADMIN");
    }

    @Test
    @DisplayName("granting SUPER_ADMIN without ROLE_SUPER_ADMIN is rejected with 403")
    void grantingSuperAdminRequiresSuperAdmin() {
        User target = user(5L, "target@example.com", "ACTIVE", role("CANDIDATE"));
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));

        var request = new UpdateUserRolesRequest();
        request.setRoles(Set.of("SUPER_ADMIN"));

        assertThatThrownBy(() -> service.updateRoles(5L, request, 2L, false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("a SUPER_ADMIN actor may grant the SUPER_ADMIN role")
    void superAdminCanGrantSuperAdmin() {
        User target = user(5L, "target@example.com", "ACTIVE", role("CANDIDATE"));
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));
        Role superAdminRole = role("SUPER_ADMIN");
        when(roleRepository.findByCode("SUPER_ADMIN")).thenReturn(Optional.of(superAdminRole));

        var request = new UpdateUserRolesRequest();
        request.setRoles(Set.of("SUPER_ADMIN"));
        var response = service.updateRoles(5L, request, 1L, true);

        assertThat(response.getRoles()).containsExactly("SUPER_ADMIN");
    }

    @Test
    @DisplayName("empty role set removes all roles (subject to protections)")
    void emptyRoleSetClearsRoles() {
        User target = user(5L, "target@example.com", "ACTIVE", role("MODERATOR"));
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));

        var request = new UpdateUserRolesRequest();
        request.setRoles(Set.of());
        var response = service.updateRoles(5L, request, 1L, true);

        assertThat(response.getRoles()).isEmpty();
        assertThat(target.getRoles()).isEmpty();
    }

    @Test
    @DisplayName("unknown role code is rejected with 400")
    void unknownRoleCodeIs400() {
        var request = new UpdateUserRolesRequest();
        request.setRoles(Set.of("HACKER"));

        assertThatThrownBy(() -> service.updateRoles(5L, request, 1L, true))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unknown role code");
    }

    @Test
    @DisplayName("empty role request body fails validation")
    void nullRolesBodyIsInvalid() {
        org.springframework.validation.beanvalidation.LocalValidatorFactoryBean validator =
                new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        var request = new UpdateUserRolesRequest();
        var violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        validator.destroy();
    }
}
