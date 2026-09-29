package com.lokmit.foundation.services.catalog.controller;

import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.security.config.CorsConfig;
import com.lokmit.foundation.security.config.JwtKeyConfig;
import com.lokmit.foundation.security.config.SecurityConfig;
import com.lokmit.foundation.security.entity.Permission;
import com.lokmit.foundation.security.entity.Role;
import com.lokmit.foundation.security.entity.User;
import com.lokmit.foundation.security.repository.UserRepository;
import com.lokmit.foundation.security.service.CustomUserDetailsService;
import com.lokmit.foundation.security.service.JwtTokenProvider;
import com.lokmit.foundation.security.util.SecurityUtils;
import com.lokmit.foundation.services.catalog.entity.ExpertiseArea;
import com.lokmit.foundation.services.catalog.entity.ServiceCategory;
import com.lokmit.foundation.services.catalog.entity.ServiceItem;
import com.lokmit.foundation.services.catalog.repository.ExpertiseAreaRepository;
import com.lokmit.foundation.services.catalog.repository.ServiceCategoryRepository;
import com.lokmit.foundation.services.catalog.repository.ServiceItemRepository;
import com.lokmit.foundation.services.catalog.service.ExpertiseAreaService;
import com.lokmit.foundation.services.catalog.service.ServiceCategoryService;
import com.lokmit.foundation.services.catalog.service.ServiceItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Admin services & expertise security and behavior matrix (A5), exercised
 * through the REAL Spring Security filter chain (real JwtAuthenticationFilter,
 * real JwtTokenProvider, real CustomUserDetailsService with mocked
 * repositories — identical to the A1–A4 matrix setups).
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>anonymous → 401 on every endpoint family (GET/POST/PATCH/DELETE)</li>
 *   <li>permission matrix: MODERATOR / CANDIDATE (no services:manage) → 403;
 *       ADMIN and SUPER_ADMIN → success</li>
 *   <li>JWT roles-claim spoofing does not elevate</li>
 *   <li>validation failures → 400; missing resources → 404; duplicates → 409;
 *       unknown category reference → 404</li>
 *   <li>safe deletion: category delete detaches services (never cascades)</li>
 *   <li>lifecycle transitions: publish/archive, archived terminal</li>
 *   <li>pagination: defaults, page-size cap, filters/search</li>
 *   <li>DTO safety: no persistence/security internals in responses</li>
 * </ul>
 */
@WebMvcTest(controllers = {AdminServiceCategoryController.class,
        AdminServiceItemController.class, AdminExpertiseAreaController.class})
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class,
        ServiceCategoryService.class, ServiceItemService.class, ExpertiseAreaService.class})
@AutoConfigureMockMvc
class AdminServicesSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceCategoryRepository serviceCategoryRepository;

    @MockitoBean
    private ServiceItemRepository serviceItemRepository;

    @MockitoBean
    private ExpertiseAreaRepository expertiseAreaRepository;

    @MockitoBean
    private UserRepository userRepository;

    private static final String CATEGORIES = ApiPaths.ADMIN_SERVICE_CATEGORIES;
    private static final String SERVICES = ApiPaths.ADMIN_SERVICES;
    private static final String EXPERTISE = ApiPaths.ADMIN_EXPERTISE_AREAS;

    private static final String SUPER_ADMIN_EMAIL = "superadmin@lokmitfoundation.org";
    private static final String ADMIN_EMAIL = "admin@lokmitfoundation.org";
    private static final String MODERATOR_EMAIL = "moderator@lokmitfoundation.org";
    private static final String CANDIDATE_EMAIL = "candidate@lokmitfoundation.org";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void resetStubs() {
        reset(serviceCategoryRepository, serviceItemRepository, expertiseAreaRepository,
                userRepository);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private User dbUser(long id, String email, Role... roles) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash("$2a$10$storedhashnotusedbyjwtfilter012345678901234567890123");
        user.setFullName("Test User");
        user.setUserType("STAFF");
        user.setStatus("ACTIVE");
        user.setEmailVerified(true);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        user.setRoles(Set.of(roles));
        return user;
    }

    private Role role(String code, String... permissionCodes) {
        Role role = new Role();
        role.setCode(code);
        role.setPermissions(Set.of(permissionCodes).stream().map(code0 -> {
            Permission permission = new Permission();
            permission.setCode(code0);
            return permission;
        }).collect(Collectors.toSet()));
        return role;
    }

    private void givenDbUser(User user) {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    private <T> void stubEmptyPage(org.springframework.data.jpa.repository.JpaSpecificationExecutor<T> repo) {
        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
    }

    private void stubSaves() {
        when(serviceCategoryRepository.save(any(ServiceCategory.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(serviceItemRepository.save(any(ServiceItem.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(expertiseAreaRepository.save(any(ExpertiseArea.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private String tokenFor(long id, String email, List<String> claimRoles) {
        return jwtTokenProvider.generateAccessToken(id, email, claimRoles);
    }

    private ServiceCategory dbCategory() {
        ServiceCategory category = new ServiceCategory();
        category.setId(1L);
        category.setName("Engineering Services");
        category.setSlug("engineering-services");
        category.setDescription("Core engineering offerings");
        category.setDisplayOrder(0);
        category.setStatus("ACTIVE");
        category.setCreatedAt(OffsetDateTime.now());
        category.setUpdatedAt(OffsetDateTime.now());
        return category;
    }

    private ServiceItem dbService(ServiceCategory category) {
        ServiceItem item = new ServiceItem();
        item.setId(10L);
        item.setSlug("structural-design");
        item.setTitle("Structural Design & Analysis");
        item.setSummary("End-to-end structural design");
        item.setDescription("Full description");
        item.setCategory(category);
        item.setDisplayOrder(0);
        item.setStatus("DRAFT");
        item.setCreatedAt(OffsetDateTime.now());
        item.setUpdatedAt(OffsetDateTime.now());
        return item;
    }

    private ExpertiseArea dbExpertise() {
        ExpertiseArea area = new ExpertiseArea();
        area.setId(4L);
        area.setSlug("bim-modelling");
        area.setName("BIM Modelling");
        area.setDescription("Building information modelling");
        area.setDisplayOrder(0);
        area.setStatus("DRAFT");
        area.setCreatedAt(OffsetDateTime.now());
        area.setUpdatedAt(OffsetDateTime.now());
        return area;
    }

    // ------------------------------------------------------------------
    // A. Anonymous → 401 (read + write probes on all three namespaces)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("anonymous access is rejected with 401 on all services namespaces")
    void anonymousIs401Everywhere() throws Exception {
        mockMvc.perform(get(CATEGORIES)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(SERVICES)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(EXPERTISE)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(CATEGORIES + "/1")).andExpect(status().isUnauthorized());

        mockMvc.perform(post(CATEGORIES).contentType("application/json")
                        .content("{\"name\":\"N\",\"slug\":\"n\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch(CATEGORIES + "/1").contentType("application/json")
                        .content("{\"name\":\"N\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(CATEGORIES + "/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(SERVICES).contentType("application/json")
                        .content("{\"slug\":\"s\",\"title\":\"T\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(SERVICES + "/1/publish"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(EXPERTISE + "/1"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // B. Permission matrix
    // ------------------------------------------------------------------

    @Test
    @DisplayName("MODERATOR and CANDIDATE (no services:manage) get 403 on all services namespaces")
    void nonAdminRolesAre403() throws Exception {
        Role moderator = role("MODERATOR", "jobs:moderate", "messages:manage");
        givenDbUser(dbUser(6L, MODERATOR_EMAIL, moderator));
        String moderatorToken = tokenFor(6L, MODERATOR_EMAIL, List.of("MODERATOR"));

        mockMvc.perform(get(CATEGORIES).header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(CATEGORIES).header("Authorization", "Bearer " + moderatorToken)
                        .contentType("application/json").content("{\"name\":\"N\",\"slug\":\"n\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(SERVICES).header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(EXPERTISE).header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());

        Role candidate = role("CANDIDATE");
        givenDbUser(dbUser(7L, CANDIDATE_EMAIL, candidate));
        String candidateToken = tokenFor(7L, CANDIDATE_EMAIL, List.of("CANDIDATE"));

        mockMvc.perform(get(SERVICES).header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(SERVICES + "/10").header("Authorization", "Bearer " + candidateToken)
                        .contentType("application/json").content("{\"title\":\"X\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN (services:manage via V12) has full access to all three namespaces")
    void adminHasFullAccess() throws Exception {
        Role admin = role("ADMIN", "content:manage", "content:publish", "settings:manage",
                "services:manage");
        givenDbUser(dbUser(8L, ADMIN_EMAIL, admin));
        String token = tokenFor(8L, ADMIN_EMAIL, List.of("ADMIN"));

        ServiceCategory category = dbCategory();
        stubEmptyPage(serviceCategoryRepository);
        stubEmptyPage(serviceItemRepository);
        stubEmptyPage(expertiseAreaRepository);
        stubSaves();
        when(serviceCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(serviceItemRepository.findById(10L)).thenReturn(Optional.of(dbService(category)));
        when(expertiseAreaRepository.findById(4L)).thenReturn(Optional.of(dbExpertise()));

        mockMvc.perform(get(CATEGORIES).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get(CATEGORIES + "/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Engineering Services"));
        mockMvc.perform(post(CATEGORIES).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"name\":\"New Cat\",\"slug\":\"new-cat\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(patch(CATEGORIES + "/1").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post(SERVICES + "/10/publish").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
        mockMvc.perform(get(EXPERTISE).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("SUPER_ADMIN has full access including deletion")
    void superAdminHasFullAccess() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "content:manage", "content:publish",
                "downloads:manage", "messages:manage", "users:manage", "jobs:manage",
                "jobs:moderate", "settings:manage", "dashboard:view", "services:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        stubSaves();
        when(serviceCategoryRepository.findById(1L)).thenReturn(Optional.of(dbCategory()));
        when(serviceItemRepository.findById(10L)).thenReturn(
                Optional.of(dbService(dbCategory())));
        when(expertiseAreaRepository.findById(4L)).thenReturn(Optional.of(dbExpertise()));

        mockMvc.perform(delete(SERVICES + "/10").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        verify(serviceItemRepository).delete(any(ServiceItem.class));
        mockMvc.perform(delete(CATEGORIES + "/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete(EXPERTISE + "/4").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("spoofed SUPER_ADMIN roles claim does not grant services:manage")
    void spoofedRoleClaimDoesNotElevate() throws Exception {
        Role candidate = role("CANDIDATE");
        givenDbUser(dbUser(9L, CANDIDATE_EMAIL, candidate));
        String token = tokenFor(9L, CANDIDATE_EMAIL, List.of("SUPER_ADMIN", "ADMIN"));

        mockMvc.perform(get(CATEGORIES).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(SERVICES + "/10").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // C. Categories: CRUD, validation, duplicates, safe delete
    // ------------------------------------------------------------------

    @Test
    @DisplayName("category validation failure → 400 (blank name, bad slug)")
    void categoryValidationFails400() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "services:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        mockMvc.perform(post(CATEGORIES).header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"name\":\"  \",\"slug\":\"ok\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(CATEGORIES).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"name\":\"Bad Slug\",\"slug\":\"Bad Slug!\"}"))
                .andExpect(status().isBadRequest());
        // Service-level status validation runs after the entity is resolved.
        when(serviceCategoryRepository.findById(1L)).thenReturn(Optional.of(dbCategory()));
        mockMvc.perform(patch(CATEGORIES + "/1").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"status\":\"RETIRED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("duplicate category name and slug → 409")
    void duplicateCategoryIs409() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "services:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        when(serviceCategoryRepository.existsByName("Engineering Services")).thenReturn(true);
        when(serviceCategoryRepository.existsBySlug("other-slug")).thenReturn(true);

        mockMvc.perform(post(CATEGORIES).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"name\":\"Engineering Services\",\"slug\":\"other-slug\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post(CATEGORIES).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"name\":\"Fresh Name\",\"slug\":\"other-slug\"}"))
                .andExpect(status().isConflict());

        // Update collision against another row → 409.
        when(serviceCategoryRepository.findById(2L)).thenReturn(Optional.of(dbCategory()));
        when(serviceCategoryRepository.existsByNameAndIdNot("Taken", 2L)).thenReturn(true);
        mockMvc.perform(patch(CATEGORIES + "/2").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"name\":\"Taken\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("missing category → 404 on get/update/delete")
    void missingCategoryIs404() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "services:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        when(serviceCategoryRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get(CATEGORIES + "/99").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch(CATEGORIES + "/99").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"name\":\"X\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(CATEGORIES + "/99").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("category delete is safe: services are detached, never cascade-deleted")
    void categoryDeleteIsSafe() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "services:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        when(serviceCategoryRepository.findById(1L)).thenReturn(Optional.of(dbCategory()));

        mockMvc.perform(delete(CATEGORIES + "/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Exactly one category delete; NO service delete may run (FK detaches).
        verify(serviceCategoryRepository).delete(any(ServiceCategory.class));
        verify(serviceItemRepository, org.mockito.Mockito.never())
                .delete(any(ServiceItem.class));
    }

    // ------------------------------------------------------------------
    // D. Services: CRUD, FK validation, duplicates, lifecycle
    // ------------------------------------------------------------------

    @Test
    @DisplayName("unknown categoryId on create/update → 404, never an orphaning insert")
    void unknownCategoryIs404() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "services:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        when(serviceCategoryRepository.findById(777L)).thenReturn(Optional.empty());

        mockMvc.perform(post(SERVICES).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"slug\":\"new-svc\",\"title\":\"New\",\"categoryId\":777}"))
                .andExpect(status().isNotFound());
        verify(serviceItemRepository, org.mockito.Mockito.never())
                .save(any(ServiceItem.class));

        when(serviceItemRepository.findById(10L)).thenReturn(
                Optional.of(dbService(dbCategory())));
        mockMvc.perform(patch(SERVICES + "/10").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"categoryId\":777}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("duplicate service slug → 409; missing service → 404")
    void serviceDuplicateAndMissing() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "services:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        when(serviceItemRepository.existsBySlug("structural-design")).thenReturn(true);
        mockMvc.perform(post(SERVICES).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"slug\":\"structural-design\",\"title\":\"Dup\"}"))
                .andExpect(status().isConflict());

        when(serviceItemRepository.findById(99L)).thenReturn(Optional.empty());
        mockMvc.perform(get(SERVICES + "/99").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(SERVICES + "/99").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("service validation failure → 400 (blank title, bad slug, negative ids)")
    void serviceValidationFails400() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "services:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        mockMvc.perform(post(SERVICES).header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"slug\":\"s\",\"title\":\" \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(SERVICES).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"slug\":\"Bad Slug\",\"title\":\"T\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(SERVICES).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"slug\":\"s2\",\"title\":\"T\",\"categoryId\":-5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("service lifecycle: publish from DRAFT, archive terminal, invalid transitions 409")
    void serviceLifecycle() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "services:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));
        stubSaves();

        ServiceItem draft = dbService(dbCategory());
        when(serviceItemRepository.findById(10L)).thenReturn(Optional.of(draft));
        mockMvc.perform(post(SERVICES + "/10/publish").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        ServiceItem published = dbService(dbCategory());
        published.setStatus("PUBLISHED");
        when(serviceItemRepository.findById(11L)).thenReturn(Optional.of(published));
        mockMvc.perform(post(SERVICES + "/11/archive").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        ServiceItem archived = dbService(dbCategory());
        archived.setStatus("ARCHIVED");
        when(serviceItemRepository.findById(12L)).thenReturn(Optional.of(archived));
        mockMvc.perform(post(SERVICES + "/12/publish").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------
    // E. Expertise areas: CRUD, validation, duplicates, lifecycle
    // ------------------------------------------------------------------

    @Test
    @DisplayName("expertise CRUD: create 201, get 200, patch 200, duplicate slug 409, missing 404")
    void expertiseCrudFlow() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "services:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));
        stubSaves();

        mockMvc.perform(post(EXPERTISE).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"slug\":\"bim-modelling\",\"name\":\"BIM Modelling\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        when(expertiseAreaRepository.findById(4L)).thenReturn(Optional.of(dbExpertise()));
        mockMvc.perform(get(EXPERTISE + "/4").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("bim-modelling"));

        mockMvc.perform(patch(EXPERTISE + "/4").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"name\":\"BIM Modeling\"}"))
                .andExpect(status().isOk());

        when(expertiseAreaRepository.existsBySlug("bim-modelling")).thenReturn(true);
        mockMvc.perform(post(EXPERTISE).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"slug\":\"bim-modelling\",\"name\":\"Other\"}"))
                .andExpect(status().isConflict());

        when(expertiseAreaRepository.findById(99L)).thenReturn(Optional.empty());
        mockMvc.perform(patch(EXPERTISE + "/99").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"name\":\"X\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("expertise validation failure → 400 and archived-terminal 409")
    void expertiseValidationAndLifecycle() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "services:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));
        stubSaves();

        mockMvc.perform(post(EXPERTISE).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"slug\":\"Bad Slug!\",\"name\":\"N\"}"))
                .andExpect(status().isBadRequest());

        ExpertiseArea archived = dbExpertise();
        archived.setStatus("ARCHIVED");
        when(expertiseAreaRepository.findById(4L)).thenReturn(Optional.of(archived));
        mockMvc.perform(post(EXPERTISE + "/4/archive").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------
    // F. Pagination, filters, DTO safety
    // ------------------------------------------------------------------

    @Test
    @DisplayName("pagination defaults, page-size cap and filter/search behavior")
    void paginationAndFilters() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "services:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        ServiceCategory category = dbCategory();

        // Default page size 20.
        stubEmptyPage(serviceCategoryRepository);
        stubEmptyPage(expertiseAreaRepository);
        mockMvc.perform(get(CATEGORIES).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(20));
        org.mockito.Mockito.verify(serviceCategoryRepository)
                .findAll(any(Specification.class), any(Pageable.class));

        // Page-size cap: 1000 → 400 via PageParams validation.
        when(serviceItemRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> {
                    Pageable requested = inv.getArgument(1, Pageable.class);
                    return new PageImpl<>(List.of(dbService(category)), requested, 1);
                });
        mockMvc.perform(get(SERVICES).header("Authorization", "Bearer " + token)
                        .queryParam("size", "1000"))
                .andExpect(status().isBadRequest());

        // Filters and search reach the repository; newest data rendered through DTOs.
        mockMvc.perform(get(SERVICES).header("Authorization", "Bearer " + token)
                        .queryParam("status", "PUBLISHED").queryParam("search", "structural")
                        .queryParam("page", "1").queryParam("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(5));
        org.mockito.Mockito.verify(serviceItemRepository)
                .findAll(any(Specification.class), any(Pageable.class));

        mockMvc.perform(get(EXPERTISE).header("Authorization", "Bearer " + token)
                        .queryParam("status", "DRAFT").queryParam("search", "bim"))
                .andExpect(status().isOk());

        // Invalid status filter → 400.
        mockMvc.perform(get(EXPERTISE).header("Authorization", "Bearer " + token)
                        .queryParam("status", "RETIRED"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DTO safety: responses carry only intended admin fields")
    void dtoSafety() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "services:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        ServiceCategory category = dbCategory();
        when(serviceCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(serviceItemRepository.findById(10L)).thenReturn(Optional.of(dbService(category)));
        when(expertiseAreaRepository.findById(4L)).thenReturn(Optional.of(dbExpertise()));

        String categoryBody = mockMvc.perform(
                        get(CATEGORIES + "/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String serviceBody = mockMvc.perform(
                        get(SERVICES + "/10").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String expertiseBody = mockMvc.perform(
                        get(EXPERTISE + "/4").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // No persistence internals, security fields or JPA noise anywhere.
        assertThat(categoryBody).doesNotContain("passwordHash", "password", "token",
                "failedLogin", "lockedUntil", "jpa", "hibernate");
        assertThat(serviceBody).doesNotContain("passwordHash", "password", "token",
                "failedLogin", "lockedUntil", "jpa", "hibernate");
        assertThat(expertiseBody).doesNotContain("passwordHash", "password", "token",
                "failedLogin", "lockedUntil", "jpa", "hibernate");

        // And the intended fields are present.
        assertThat(categoryBody).contains("slug", "displayOrder", "status");
        assertThat(serviceBody).contains("category");
        assertThat(expertiseBody).contains("slug", "displayOrder", "status");
    }
}
