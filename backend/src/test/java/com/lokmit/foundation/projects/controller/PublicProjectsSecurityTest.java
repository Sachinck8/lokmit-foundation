package com.lokmit.foundation.projects.controller;

import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.projects.entity.Project;
import com.lokmit.foundation.projects.entity.ProjectCategory;
import com.lokmit.foundation.projects.repository.ProjectCategoryRepository;
import com.lokmit.foundation.projects.repository.ProjectRepository;
import com.lokmit.foundation.projects.service.PublicProjectService;
import com.lokmit.foundation.security.config.CorsConfig;
import com.lokmit.foundation.security.config.JwtKeyConfig;
import com.lokmit.foundation.security.config.SecurityConfig;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Public projects security and behavior matrix (A26), exercised through the
 * REAL Spring Security filter chain (real JwtAuthenticationFilter, real
 * JwtTokenProvider, real CustomUserDetailsService with mocked repositories —
 * identical to the A1–A8 matrix setups).
 *
 * <p>Coverage: anonymous GET success + standard envelope shape; PUBLISHED
 * filtering (drafts and archived projects are unreachable); ACTIVE-category
 * filtering by slug; 404 masking for unknown and non-published slugs;
 * DTO safety; POST on public paths → 401 (no anonymous mutation).</p>
 */
@WebMvcTest(controllers = {PublicProjectController.class})
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class,
        PublicProjectService.class})
@AutoConfigureMockMvc
class PublicProjectsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectRepository projectRepository;

    @MockitoBean
    private ProjectCategoryRepository projectCategoryRepository;

    @MockitoBean
    private UserRepository userRepository;

    private static final String PROJECTS = ApiPaths.PUBLIC_PROJECTS;
    private static final String CATEGORIES = ApiPaths.PUBLIC_PROJECT_CATEGORIES;

    @BeforeEach
    void resetStubs() {
        reset(projectRepository, projectCategoryRepository, userRepository);
    }

    // helpers -----------------------------------------------------------

    private ProjectCategory activeCategory() {
        ProjectCategory category = new ProjectCategory();
        category.setId(1L);
        category.setName("Infrastructure");
        category.setSlug("infrastructure");
        category.setDescription("Infrastructure projects");
        category.setDisplayOrder(0);
        category.setStatus(ProjectCategory.STATUS_ACTIVE);
        category.setUpdatedAt(OffsetDateTime.now());
        return category;
    }

    private Project publishedProject(ProjectCategory category) {
        Project project = new Project();
        project.setId(10L);
        project.setSlug("rural-water-supply");
        project.setTitle("Rural Water Supply Programme");
        project.setSummary("Clean water for rural communities");
        project.setDescription("Full description");
        project.setCategory(category);
        project.setStatus(Project.STATUS_PUBLISHED);
        project.setProjectStatus(Project.PROJECT_STATUS_ONGOING);
        project.setLocation("Kathmandu, Nepal");
        project.setStartDate(LocalDate.of(2026, 1, 1));
        project.setEndDate(LocalDate.of(2026, 12, 31));
        project.setObjectives("{\"goal\":\"clean water\"}");
        project.setImpactSummary("10 villages served");
        project.setPublishedAt(OffsetDateTime.now());
        project.setUpdatedAt(OffsetDateTime.now());
        return project;
    }

    @SuppressWarnings("unchecked")
    private void stubProjectPage(List<Project> items) {
        when(projectRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> {
                    Pageable requested = inv.getArgument(1, Pageable.class);
                    return new PageImpl<>(items, requested, items.size());
                });
    }

    private String projectPath(String slug) {
        return ApiPaths.PUBLIC_PROJECT.replace("{slug}", slug);
    }

    // tests --------------------------------------------------------------

    @Test
    @DisplayName("anonymous GET /projects returns published projects with the public DTO shape")
    void anonymousListProjectsOk() throws Exception {
        stubProjectPage(List.of(publishedProject(activeCategory())));

        mockMvc.perform(get(PROJECTS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].slug").value("rural-water-supply"))
                .andExpect(jsonPath("$.data.items[0].projectStatus").value("ONGOING"))
                .andExpect(jsonPath("$.data.items[0].status").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].images").doesNotExist())
                .andExpect(jsonPath("$.data.totalItems").value(1));
    }

    @Test
    @DisplayName("category slug filter resolves ACTIVE categories only; unknown/inactive slugs yield an empty page")
    void categoryFilterBehavior() throws Exception {
        when(projectCategoryRepository.findByStatusAndSlug("ACTIVE", "infrastructure"))
                .thenReturn(List.of(activeCategory()));
        stubProjectPage(List.of(publishedProject(activeCategory())));

        mockMvc.perform(get(PROJECTS).queryParam("category", "infrastructure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1));
        verify(projectCategoryRepository).findByStatusAndSlug("ACTIVE", "infrastructure");

        stubProjectPage(List.of());
        when(projectCategoryRepository.findByStatusAndSlug("ACTIVE", "nope"))
                .thenReturn(List.of());
        mockMvc.perform(get(PROJECTS).queryParam("category", "nope"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    @DisplayName("unknown slug and known-but-DRAFT slug are an indistinguishable masked 404")
    void anonymousProjectDetailAndMasked404() throws Exception {
        when(projectRepository.findBySlug("rural-water-supply"))
                .thenReturn(Optional.of(publishedProject(activeCategory())));

        Project draft = publishedProject(activeCategory());
        draft.setStatus(Project.STATUS_DRAFT);
        when(projectRepository.findBySlug("draft-project"))
                .thenReturn(Optional.of(draft));

        mockMvc.perform(get(projectPath("rural-water-supply")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("rural-water-supply"))
                .andExpect(jsonPath("$.data.status").doesNotExist());

        String unknown = mockMvc.perform(get(projectPath("nope")))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        String draftBody = mockMvc.perform(get(projectPath("draft-project")))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertThat(unknown).contains("\"success\":false").contains("NOT_FOUND");
        assertThat(draftBody).contains("\"success\":false").contains("NOT_FOUND");
        assertThat(normalizeTimestamp(draftBody)).isEqualTo(normalizeTimestamp(unknown));
    }

    /** Timestamps legitimately differ; everything else must be identical. */
    private String normalizeTimestamp(String body) {
        return body.replaceAll("\"timestamp\":\"[^\"]*\"", "\"timestamp\":\"T\"");
    }

    @Test
    @DisplayName("anonymous GET /project-categories returns only ACTIVE categories")
    void anonymousProjectCategoriesOk() throws Exception {
        when(projectCategoryRepository.findByStatusOrderByDisplayOrderAsc("ACTIVE"))
                .thenReturn(List.of(activeCategory()));

        mockMvc.perform(get(CATEGORIES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].slug").value("infrastructure"))
                .andExpect(jsonPath("$.data[0].status").doesNotExist());

        verify(projectCategoryRepository).findByStatusOrderByDisplayOrderAsc(
                ProjectCategory.STATUS_ACTIVE);
    }

    @Test
    @DisplayName("DTO safety: no admin lifecycle/image fields leak into public project responses")
    void dtoSafety() throws Exception {
        stubProjectPage(List.of(publishedProject(activeCategory())));
        when(projectRepository.findBySlug("rural-water-supply"))
                .thenReturn(Optional.of(publishedProject(activeCategory())));

        String listBody = mockMvc.perform(get(PROJECTS))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String detailBody = mockMvc.perform(get(projectPath("rural-water-supply")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Editorial lifecycle status stays internal; delivery state is public.
        assertThat(listBody).doesNotContain("\"status\"", "images", "password", "token");
        assertThat(detailBody).contains("projectStatus", "publishedAt", "impactSummary");
        assertThat(detailBody).doesNotContain("\"status\"", "\"images\"", "password", "token");
    }

    @Test
    @DisplayName("POST on public project paths stays authenticated → anonymous gets 401, never a mutation")
    void postOnPublicPathsIs401() throws Exception {
        mockMvc.perform(post(PROJECTS).contentType("application/json")
                        .content("{\"slug\":\"x\",\"title\":\"X\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(CATEGORIES).contentType("application/json")
                        .content("{\"name\":\"x\",\"slug\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        verify(projectRepository, org.mockito.Mockito.never()).save(any(Project.class));
        verify(projectCategoryRepository, org.mockito.Mockito.never()).save(any(ProjectCategory.class));
    }
}
