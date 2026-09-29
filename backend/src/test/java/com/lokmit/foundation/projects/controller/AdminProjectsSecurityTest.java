package com.lokmit.foundation.projects.controller;

import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.projects.entity.Project;
import com.lokmit.foundation.projects.entity.ProjectCategory;
import com.lokmit.foundation.projects.entity.ProjectImage;
import com.lokmit.foundation.projects.repository.ProjectCategoryRepository;
import com.lokmit.foundation.projects.repository.ProjectImageRepository;
import com.lokmit.foundation.projects.repository.ProjectRepository;
import com.lokmit.foundation.projects.service.ProjectCategoryService;
import com.lokmit.foundation.projects.service.ProjectImageService;
import com.lokmit.foundation.projects.service.ProjectService;
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

import java.time.LocalDate;
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
 * Admin projects security and behavior matrix (A6), exercised through the
 * REAL Spring Security filter chain (real JwtAuthenticationFilter, real
 * JwtTokenProvider, real CustomUserDetailsService with mocked repositories
 * — identical to the A1–A5 matrix setups).
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>anonymous → 401 on every endpoint family (GET/POST/PATCH/DELETE)</li>
 *   <li>permission matrix: MODERATOR / CANDIDATE (no projects:manage) → 403;
 *       ADMIN and SUPER_ADMIN → success</li>
 *   <li>JWT roles-claim spoofing does not elevate</li>
 *   <li>validation failures → 400; missing resources → 404; duplicates → 409;
 *       unknown category/project references → 404; chk_projects_dates → 400</li>
 *   <li>safe deletion: category delete detaches projects (never cascades);
 *       project delete removes owned image metadata via the FK cascade;
 *       mismatched project/image pair → 404</li>
 *   <li>lifecycle transitions: publish stamps published_at, archive terminal</li>
 *   <li>pagination: defaults, page-size cap, filters/search</li>
 *   <li>DTO safety: no persistence/security internals in responses</li>
 * </ul>
 */
@WebMvcTest(controllers = {AdminProjectCategoryController.class, AdminProjectController.class})
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class,
        ProjectCategoryService.class, ProjectService.class, ProjectImageService.class})
@AutoConfigureMockMvc
class AdminProjectsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectCategoryRepository projectCategoryRepository;

    @MockitoBean
    private ProjectRepository projectRepository;

    @MockitoBean
    private ProjectImageRepository projectImageRepository;

    @MockitoBean
    private UserRepository userRepository;

    private static final String CATEGORIES = ApiPaths.ADMIN_PROJECT_CATEGORIES;
    private static final String PROJECTS = ApiPaths.ADMIN_PROJECTS;

    private static final String SUPER_ADMIN_EMAIL = "superadmin@lokmitfoundation.org";
    private static final String ADMIN_EMAIL = "admin@lokmitfoundation.org";
    private static final String MODERATOR_EMAIL = "moderator@lokmitfoundation.org";
    private static final String CANDIDATE_EMAIL = "candidate@lokmitfoundation.org";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void resetStubs() {
        reset(projectCategoryRepository, projectRepository, projectImageRepository,
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

    @SuppressWarnings("unchecked")
    private <T, R extends org.springframework.data.jpa.repository.JpaSpecificationExecutor<T> & org.springframework.data.repository.CrudRepository<T, ?>>
    void stubEmptyPage(R repo, Class<T> type) {
        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
    }

    private void stubSaves() {
        when(projectCategoryRepository.save(any(ProjectCategory.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(projectRepository.save(any(Project.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(projectImageRepository.save(any(ProjectImage.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private String tokenFor(long id, String email, List<String> claimRoles) {
        return jwtTokenProvider.generateAccessToken(id, email, claimRoles);
    }

    private ProjectCategory dbCategory() {
        ProjectCategory category = new ProjectCategory();
        category.setId(1L);
        category.setName("Infrastructure");
        category.setSlug("infrastructure");
        category.setDescription("Infrastructure projects");
        category.setDisplayOrder(0);
        category.setStatus("ACTIVE");
        category.setCreatedAt(OffsetDateTime.now());
        category.setUpdatedAt(OffsetDateTime.now());
        return category;
    }

    private Project dbProject(ProjectCategory category) {
        Project project = new Project();
        project.setId(10L);
        project.setSlug("rural-water-supply");
        project.setTitle("Rural Water Supply Programme");
        project.setSummary("Clean water for rural communities");
        project.setDescription("Full description");
        project.setCategory(category);
        project.setStatus("DRAFT");
        project.setProjectStatus("ONGOING");
        project.setLocation("Kathmandu, Nepal");
        project.setStartDate(LocalDate.of(2026, 1, 1));
        project.setEndDate(LocalDate.of(2026, 12, 31));
        project.setObjectives("{\"goal\":\"clean water\"}");
        project.setImpactSummary("10 villages served");
        project.setCreatedAt(OffsetDateTime.now());
        project.setUpdatedAt(OffsetDateTime.now());
        return project;
    }

    private ProjectImage dbImage(Project project) {
        ProjectImage image = new ProjectImage();
        image.setId(20L);
        image.setProject(project);
        image.setImageUrl("https://cdn.example.org/images/site-visit-01.jpg");
        image.setAltText("Site visit");
        image.setCaption("First site visit");
        image.setDisplayOrder(0);
        image.setCreatedAt(OffsetDateTime.now());
        return image;
    }

    // ------------------------------------------------------------------
    // A. Anonymous → 401 (read + write probes on both namespaces)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("anonymous access is rejected with 401 on all projects namespaces")
    void anonymousIs401Everywhere() throws Exception {
        mockMvc.perform(get(CATEGORIES)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(PROJECTS)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(CATEGORIES + "/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(PROJECTS + "/10")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(PROJECTS + "/10/images")).andExpect(status().isUnauthorized());

        mockMvc.perform(post(CATEGORIES).contentType("application/json")
                        .content("{\"name\":\"N\",\"slug\":\"n\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch(CATEGORIES + "/1").contentType("application/json")
                        .content("{\"name\":\"N\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(CATEGORIES + "/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(PROJECTS).contentType("application/json")
                        .content("{\"slug\":\"s\",\"title\":\"T\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(PROJECTS + "/10/publish"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(PROJECTS + "/10/images").contentType("application/json")
                        .content("{\"imageUrl\":\"https://x.test/a.jpg\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(PROJECTS + "/10/images/20"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // B. Permission matrix
    // ------------------------------------------------------------------

    @Test
    @DisplayName("MODERATOR and CANDIDATE (no projects:manage) get 403 on all projects namespaces")
    void nonAdminRolesAre403() throws Exception {
        Role moderator = role("MODERATOR", "jobs:moderate", "messages:manage");
        givenDbUser(dbUser(6L, MODERATOR_EMAIL, moderator));
        String moderatorToken = tokenFor(6L, MODERATOR_EMAIL, List.of("MODERATOR"));

        mockMvc.perform(get(CATEGORIES).header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(CATEGORIES).header("Authorization", "Bearer " + moderatorToken)
                        .contentType("application/json").content("{\"name\":\"N\",\"slug\":\"n\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(PROJECTS).header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(PROJECTS + "/10").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());

        Role candidate = role("CANDIDATE");
        givenDbUser(dbUser(7L, CANDIDATE_EMAIL, candidate));
        String candidateToken = tokenFor(7L, CANDIDATE_EMAIL, List.of("CANDIDATE"));

        mockMvc.perform(get(PROJECTS).header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(PROJECTS + "/10").header("Authorization", "Bearer " + candidateToken)
                        .contentType("application/json").content("{\"title\":\"X\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN (projects:manage via V13) has full access to both namespaces")
    void adminHasFullAccess() throws Exception {
        Role admin = role("ADMIN", "content:manage", "content:publish", "settings:manage",
                "services:manage", "projects:manage");
        givenDbUser(dbUser(8L, ADMIN_EMAIL, admin));
        String token = tokenFor(8L, ADMIN_EMAIL, List.of("ADMIN"));

        ProjectCategory category = dbCategory();
        stubEmptyPage(projectCategoryRepository, ProjectCategory.class);
        stubEmptyPage(projectRepository, Project.class);
        stubSaves();
        when(projectCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(dbProject(category)));

        mockMvc.perform(get(CATEGORIES).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get(CATEGORIES + "/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Infrastructure"));
        mockMvc.perform(post(CATEGORIES).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"name\":\"New Cat\",\"slug\":\"new-cat\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(patch(CATEGORIES + "/1").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post(PROJECTS + "/10/publish").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.publishedAt").isNotEmpty());
        mockMvc.perform(get(PROJECTS).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("SUPER_ADMIN has full access including deletion")
    void superAdminHasFullAccess() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "content:manage", "content:publish",
                "downloads:manage", "messages:manage", "users:manage", "jobs:manage",
                "jobs:moderate", "settings:manage", "dashboard:view", "services:manage",
                "projects:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        ProjectCategory category = dbCategory();
        Project project = dbProject(category);
        stubSaves();
        when(projectCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(projectImageRepository.findById(20L)).thenReturn(Optional.of(dbImage(project)));

        mockMvc.perform(delete(PROJECTS + "/10").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        verify(projectRepository).delete(any(Project.class));
        mockMvc.perform(delete(CATEGORIES + "/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete(PROJECTS + "/10/images/20")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        verify(projectImageRepository).delete(any(ProjectImage.class));
    }

    @Test
    @DisplayName("spoofed SUPER_ADMIN roles claim does not grant projects:manage")
    void spoofedRoleClaimDoesNotElevate() throws Exception {
        Role candidate = role("CANDIDATE");
        givenDbUser(dbUser(9L, CANDIDATE_EMAIL, candidate));
        String token = tokenFor(9L, CANDIDATE_EMAIL, List.of("SUPER_ADMIN", "ADMIN"));

        mockMvc.perform(get(CATEGORIES).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(PROJECTS + "/10").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // C. Categories: CRUD, validation, duplicates, safe delete
    // ------------------------------------------------------------------

    @Test
    @DisplayName("category validation failure → 400 (blank name, bad slug, bad status)")
    void categoryValidationFails400() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "projects:manage");
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
        when(projectCategoryRepository.findById(1L)).thenReturn(Optional.of(dbCategory()));
        mockMvc.perform(patch(CATEGORIES + "/1").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"status\":\"RETIRED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("duplicate category name and slug → 409")
    void duplicateCategoryIs409() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "projects:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        when(projectCategoryRepository.existsByName("Infrastructure")).thenReturn(true);
        when(projectCategoryRepository.existsBySlug("other-slug")).thenReturn(true);

        mockMvc.perform(post(CATEGORIES).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"name\":\"Infrastructure\",\"slug\":\"other-slug\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post(CATEGORIES).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"name\":\"Fresh Name\",\"slug\":\"other-slug\"}"))
                .andExpect(status().isConflict());

        // Update collision against another row → 409.
        when(projectCategoryRepository.findById(2L)).thenReturn(Optional.of(dbCategory()));
        when(projectCategoryRepository.existsByNameAndIdNot("Taken", 2L)).thenReturn(true);
        mockMvc.perform(patch(CATEGORIES + "/2").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"name\":\"Taken\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("missing category → 404 on get/update/delete")
    void missingCategoryIs404() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "projects:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        when(projectCategoryRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get(CATEGORIES + "/99").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch(CATEGORIES + "/99").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"name\":\"X\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(CATEGORIES + "/99").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("category delete is safe: projects are detached, never cascade-deleted")
    void categoryDeleteIsSafe() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "projects:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        when(projectCategoryRepository.findById(1L)).thenReturn(Optional.of(dbCategory()));
        when(projectRepository.countByCategoryId(1L)).thenReturn(3L);

        mockMvc.perform(delete(CATEGORIES + "/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Exactly one category delete; NO project delete may run (FK detaches).
        verify(projectCategoryRepository).delete(any(ProjectCategory.class));
        verify(projectRepository, org.mockito.Mockito.never()).delete(any(Project.class));
    }

    // ------------------------------------------------------------------
    // D. Projects: CRUD, FK validation, duplicates, lifecycle, dates
    // ------------------------------------------------------------------

    @Test
    @DisplayName("unknown categoryId on create/update → 404, never an orphaning insert")
    void unknownCategoryIs404() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "projects:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        when(projectCategoryRepository.findById(777L)).thenReturn(Optional.empty());

        mockMvc.perform(post(PROJECTS).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"slug\":\"new-proj\",\"title\":\"New\",\"categoryId\":777}"))
                .andExpect(status().isNotFound());
        verify(projectRepository, org.mockito.Mockito.never()).save(any(Project.class));

        when(projectRepository.findById(10L)).thenReturn(Optional.of(dbProject(dbCategory())));
        mockMvc.perform(patch(PROJECTS + "/10").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"categoryId\":777}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("duplicate project slug → 409; missing project → 404")
    void projectDuplicateAndMissing() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "projects:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        when(projectRepository.existsBySlug("rural-water-supply")).thenReturn(true);
        mockMvc.perform(post(PROJECTS).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"slug\":\"rural-water-supply\",\"title\":\"Dup\"}"))
                .andExpect(status().isConflict());

        when(projectRepository.findById(99L)).thenReturn(Optional.empty());
        mockMvc.perform(get(PROJECTS + "/99").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch(PROJECTS + "/99").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"title\":\"X\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(PROJECTS + "/99").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("project validation failure → 400 (blank title, bad slug, bad enums, date pair)")
    void projectValidationFails400() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "projects:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        mockMvc.perform(post(PROJECTS).header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"slug\":\"p\",\"title\":\" \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(PROJECTS).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"slug\":\"Bad Slug\",\"title\":\"T\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(PROJECTS).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"slug\":\"p2\",\"title\":\"T\",\"projectStatus\":\"PAUSED\"}"))
                .andExpect(status().isBadRequest());
        // chk_projects_dates: end before start → 400 before the DB sees it.
        mockMvc.perform(post(PROJECTS).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"slug\":\"p3\",\"title\":\"T\",\"startDate\":\"2026-06-01\","
                                + "\"endDate\":\"2026-01-01\"}"))
                .andExpect(status().isBadRequest());
        // Malformed objectives JSON → 400.
        mockMvc.perform(post(PROJECTS).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"slug\":\"p4\",\"title\":\"T\",\"objectives\":\"{not-json\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("project lifecycle: publish stamps published_at, archive terminal, invalid 409")
    void projectLifecycle() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "projects:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));
        stubSaves();

        Project draft = dbProject(dbCategory());
        when(projectRepository.findById(10L)).thenReturn(Optional.of(draft));
        mockMvc.perform(post(PROJECTS + "/10/publish").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.publishedAt").isNotEmpty());

        Project published = dbProject(dbCategory());
        published.setStatus("PUBLISHED");
        when(projectRepository.findById(11L)).thenReturn(Optional.of(published));
        mockMvc.perform(post(PROJECTS + "/11/archive").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        Project archived = dbProject(dbCategory());
        archived.setStatus("ARCHIVED");
        when(projectRepository.findById(12L)).thenReturn(Optional.of(archived));
        mockMvc.perform(post(PROJECTS + "/12/publish").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
        mockMvc.perform(post(PROJECTS + "/12/archive").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH cannot create an end-before-start pair via the untouched other side")
    void patchDatePairStaysValid() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "projects:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));
        stubSaves();

        Project project = dbProject(dbCategory()); // start 2026-01-01, end 2026-12-31
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        // Moving the start date past the existing end date → 400.
        mockMvc.perform(patch(PROJECTS + "/10").header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"startDate\":\"2027-06-01\"}"))
                .andExpect(status().isBadRequest());

        // Moving the end date before the existing start date → 400.
        mockMvc.perform(patch(PROJECTS + "/10").header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"endDate\":\"2025-01-01\"}"))
                .andExpect(status().isBadRequest());

        // Clearing the end date is always fine.
        mockMvc.perform(patch(PROJECTS + "/10").header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"endDate\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endDate").doesNotExist());
    }

    // ------------------------------------------------------------------
    // E. Images: metadata CRUD, ownership, duplicates
    // ------------------------------------------------------------------

    @Test
    @DisplayName("image metadata CRUD: create 201, list, get, patch, duplicate URL 409, missing 404")
    void imageCrudFlow() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "projects:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));
        stubSaves();

        Project project = dbProject(dbCategory());
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(projectImageRepository.findByProjectId(org.mockito.ArgumentMatchers.eq(10L),
                any(Pageable.class)))
                .thenAnswer(inv -> {
                    Pageable requested = inv.getArgument(1, Pageable.class);
                    return new PageImpl<>(List.of(dbImage(project)), requested, 1);
                });
        when(projectImageRepository.findById(20L)).thenReturn(Optional.of(dbImage(project)));

        mockMvc.perform(post(PROJECTS + "/10/images").header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"imageUrl\":\"https://cdn.example.org/images/a.jpg\","
                                + "\"altText\":\"A\",\"displayOrder\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.imageUrl").value("https://cdn.example.org/images/a.jpg"))
                .andExpect(jsonPath("$.data.project.id").value(10));

        mockMvc.perform(get(PROJECTS + "/10/images").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].imageUrl")
                        .value("https://cdn.example.org/images/site-visit-01.jpg"));

        mockMvc.perform(get(PROJECTS + "/10/images/20").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageUrl")
                        .value("https://cdn.example.org/images/site-visit-01.jpg"));

        mockMvc.perform(patch(PROJECTS + "/10/images/20").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"caption\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.caption").value("Updated"));

        when(projectImageRepository.existsByProjectIdAndImageUrl(10L,
                "https://cdn.example.org/images/site-visit-01.jpg")).thenReturn(true);
        mockMvc.perform(post(PROJECTS + "/10/images").header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"imageUrl\":\"https://cdn.example.org/images/site-visit-01.jpg\"}"))
                .andExpect(status().isConflict());

        when(projectRepository.findById(99L)).thenReturn(Optional.empty());
        mockMvc.perform(post(PROJECTS + "/99/images").header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"imageUrl\":\"https://cdn.example.org/images/x.jpg\"}"))
                .andExpect(status().isNotFound());

        when(projectImageRepository.findById(99L)).thenReturn(Optional.empty());
        mockMvc.perform(get(PROJECTS + "/10/images/99").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("mismatched project/image pair → 404 (cross-project access denied)")
    void mismatchedImageOwnershipIs404() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "projects:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));
        stubSaves();

        Project project = dbProject(dbCategory());
        when(projectImageRepository.findById(20L)).thenReturn(Optional.of(dbImage(project)));

        // Image 20 belongs to project 10 — project 11 must not see it.
        mockMvc.perform(get(PROJECTS + "/11/images/20").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch(PROJECTS + "/11/images/20").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"caption\":\"X\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(PROJECTS + "/11/images/20")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        verify(projectImageRepository, org.mockito.Mockito.never()).delete(any(ProjectImage.class));
    }

    // ------------------------------------------------------------------
    // F. Pagination, filters, DTO safety
    // ------------------------------------------------------------------

    @Test
    @DisplayName("pagination defaults, page-size cap and filter/search behavior")
    void paginationAndFilters() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "projects:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        // Default page size 20.
        stubEmptyPage(projectCategoryRepository, ProjectCategory.class);
        mockMvc.perform(get(CATEGORIES).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(20));
        verify(projectCategoryRepository).findAll(any(Specification.class), any(Pageable.class));

        // Page-size cap: 1000 → 400 via PageParams validation.
        stubEmptyPage(projectRepository, Project.class);
        mockMvc.perform(get(PROJECTS).header("Authorization", "Bearer " + token)
                        .queryParam("size", "1000"))
                .andExpect(status().isBadRequest());

        // Filters and search reach the repository; page honored.
        when(projectRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> {
                    Pageable requested = inv.getArgument(1, Pageable.class);
                    return new PageImpl<>(List.of(dbProject(dbCategory())), requested, 1);
                });
        mockMvc.perform(get(PROJECTS).header("Authorization", "Bearer " + token)
                        .queryParam("status", "PUBLISHED").queryParam("projectStatus", "ONGOING")
                        .queryParam("search", "water").queryParam("page", "1")
                        .queryParam("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(5));
        verify(projectRepository).findAll(any(Specification.class), any(Pageable.class));

        // Invalid status filters → 400.
        mockMvc.perform(get(PROJECTS).header("Authorization", "Bearer " + token)
                        .queryParam("status", "RETIRED"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(PROJECTS).header("Authorization", "Bearer " + token)
                        .queryParam("projectStatus", "PAUSED"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(CATEGORIES).header("Authorization", "Bearer " + token)
                        .queryParam("status", "RETIRED"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DTO safety: responses carry only intended admin fields")
    void dtoSafety() throws Exception {
        Role superAdmin = role("SUPER_ADMIN", "projects:manage");
        givenDbUser(dbUser(2L, SUPER_ADMIN_EMAIL, superAdmin));
        String token = tokenFor(2L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        ProjectCategory category = dbCategory();
        Project project = dbProject(category);
        when(projectCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(projectImageRepository.findById(20L)).thenReturn(Optional.of(dbImage(project)));

        String categoryBody = mockMvc.perform(
                        get(CATEGORIES + "/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String projectBody = mockMvc.perform(
                        get(PROJECTS + "/10").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String imageBody = mockMvc.perform(
                        get(PROJECTS + "/10/images/20").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // No persistence internals, security fields or JPA noise anywhere.
        assertThat(categoryBody).doesNotContain("passwordHash", "password", "token",
                "failedLogin", "lockedUntil", "jpa", "hibernate");
        assertThat(projectBody).doesNotContain("passwordHash", "password", "token",
                "failedLogin", "lockedUntil", "jpa", "hibernate");
        assertThat(imageBody).doesNotContain("passwordHash", "password", "token",
                "failedLogin", "lockedUntil", "jpa", "hibernate");

        // And the intended fields are present.
        assertThat(categoryBody).contains("slug", "displayOrder", "status");
        assertThat(projectBody).contains("category", "projectStatus", "publishedAt");
        assertThat(imageBody).contains("imageUrl", "altText", "caption");
    }
}
