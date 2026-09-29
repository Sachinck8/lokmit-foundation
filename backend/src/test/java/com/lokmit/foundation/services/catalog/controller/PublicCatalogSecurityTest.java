package com.lokmit.foundation.services.catalog.controller;

import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.security.config.CorsConfig;
import com.lokmit.foundation.security.config.JwtKeyConfig;
import com.lokmit.foundation.security.config.SecurityConfig;
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
import com.lokmit.foundation.services.catalog.service.PublicCatalogService;
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
 * Public catalog security and behavior matrix (A26), exercised through the
 * REAL Spring Security filter chain (real JwtAuthenticationFilter, real
 * JwtTokenProvider, real CustomUserDetailsService with mocked repositories —
 * identical to the A1–A8 matrix setups).
 *
 * <p>Coverage: anonymous GET success + standard envelope shape; status
 * filtering (only PUBLISHED/ACTIVE are reachable); 404 masking for unknown
 * and non-published slugs (indistinguishable); DTO safety; POST on public
 * paths → 401 (no anonymous mutation); pagination validation; rate-limit
 * scope (public catalog GETs sit outside the I-6 limiter, like /jobs).</p>
 */
@WebMvcTest(controllers = {PublicCatalogController.class})
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class,
        PublicCatalogService.class})
@AutoConfigureMockMvc
class PublicCatalogSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceItemRepository serviceItemRepository;

    @MockitoBean
    private ServiceCategoryRepository serviceCategoryRepository;

    @MockitoBean
    private ExpertiseAreaRepository expertiseAreaRepository;

    @MockitoBean
    private UserRepository userRepository;

    private static final String SERVICES = ApiPaths.PUBLIC_SERVICES;
    private static final String CATEGORIES = ApiPaths.PUBLIC_SERVICE_CATEGORIES;
    private static final String EXPERTISE = ApiPaths.PUBLIC_EXPERTISE_AREAS;

    @BeforeEach
    void resetStubs() {
        reset(serviceItemRepository, serviceCategoryRepository, expertiseAreaRepository,
                userRepository);
    }

    // helpers -----------------------------------------------------------

    private ServiceItem publishedService() {
        ServiceItem item = new ServiceItem();
        item.setId(10L);
        item.setSlug("alpha-service");
        item.setTitle("Alpha Service");
        item.setSummary("Alpha summary");
        item.setDescription("Alpha full description");
        item.setDisplayOrder(1);
        item.setStatus(ServiceItem.STATUS_PUBLISHED);
        item.setUpdatedAt(OffsetDateTime.now());
        return item;
    }

    private ServiceCategory activeCategory() {
        ServiceCategory category = new ServiceCategory();
        category.setId(1L);
        category.setName("Consultancy");
        category.setSlug("consultancy");
        category.setDescription("Consultancy services");
        category.setDisplayOrder(0);
        category.setStatus(ServiceCategory.STATUS_ACTIVE);
        category.setUpdatedAt(OffsetDateTime.now());
        return category;
    }

    private ExpertiseArea publishedArea() {
        ExpertiseArea area = new ExpertiseArea();
        area.setId(5L);
        area.setSlug("skill-development");
        area.setName("Skill Development");
        area.setDescription("Skill development programs");
        area.setDisplayOrder(0);
        area.setStatus(ExpertiseArea.STATUS_PUBLISHED);
        area.setUpdatedAt(OffsetDateTime.now());
        return area;
    }

    @SuppressWarnings("unchecked")
    private void stubServicePage(List<ServiceItem> items) {
        when(serviceItemRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> {
                    Pageable requested = inv.getArgument(1, Pageable.class);
                    return new PageImpl<>(items, requested, items.size());
                });
    }

    private String servicePath(String slug) {
        return ApiPaths.PUBLIC_SERVICE.replace("{slug}", slug);
    }

    // tests --------------------------------------------------------------

    @Test
    @DisplayName("anonymous GET /services returns published services in the standard envelope")
    void anonymousListServicesOk() throws Exception {
        stubServicePage(List.of(publishedService()));

        mockMvc.perform(get(SERVICES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].slug").value("alpha-service"))
                .andExpect(jsonPath("$.data.items[0].title").value("Alpha Service"))
                .andExpect(jsonPath("$.data.items[0].status").doesNotExist())
                .andExpect(jsonPath("$.data.totalItems").value(1));
    }

    @Test
    @DisplayName("unknown slug and known-but-DRAFT slug are an indistinguishable masked 404")
    void anonymousServiceDetailAndMasked404() throws Exception {
        when(serviceItemRepository.findBySlug("alpha-service"))
                .thenReturn(Optional.of(publishedService()));

        ServiceItem draft = publishedService();
        draft.setStatus(ServiceItem.STATUS_DRAFT);
        when(serviceItemRepository.findBySlug("draft-service"))
                .thenReturn(Optional.of(draft));

        mockMvc.perform(get(servicePath("alpha-service")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("alpha-service"))
                .andExpect(jsonPath("$.data.status").doesNotExist());

        String unknown = mockMvc.perform(get(servicePath("nope")))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        String draftBody = mockMvc.perform(get(servicePath("draft-service")))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        // Same envelope verdict: identical error code, indistinguishable bodies
        // modulo the timestamp.
        assertThat(unknown).contains("\"success\":false").contains("NOT_FOUND");
        assertThat(draftBody).contains("\"success\":false").contains("NOT_FOUND");
        assertThat(normalizeTimestamp(draftBody)).isEqualTo(normalizeTimestamp(unknown));
    }

    /** Timestamps legitimately differ; everything else must be identical. */
    private String normalizeTimestamp(String body) {
        return body.replaceAll("\"timestamp\":\"[^\"]*\"", "\"timestamp\":\"T\"");
    }

    @Test
    @DisplayName("anonymous GET /service-categories returns only ACTIVE categories")
    void anonymousServiceCategoriesOk() throws Exception {
        when(serviceCategoryRepository.findByStatusOrderByDisplayOrderAsc("ACTIVE"))
                .thenReturn(List.of(activeCategory()));

        mockMvc.perform(get(CATEGORIES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].slug").value("consultancy"))
                .andExpect(jsonPath("$.data[0].status").doesNotExist());

        // The service queries by the ACTIVE constant only — inactive never leaks.
        verify(serviceCategoryRepository).findByStatusOrderByDisplayOrderAsc(
                ServiceCategory.STATUS_ACTIVE);
    }

    @Test
    @DisplayName("anonymous GET /expertise-areas returns only PUBLISHED areas")
    void anonymousExpertiseAreasOk() throws Exception {
        when(expertiseAreaRepository.findByStatusOrderByDisplayOrderAsc("PUBLISHED"))
                .thenReturn(List.of(publishedArea()));

        mockMvc.perform(get(EXPERTISE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].slug").value("skill-development"))
                .andExpect(jsonPath("$.data[0].status").doesNotExist());

        verify(expertiseAreaRepository).findByStatusOrderByDisplayOrderAsc(
                ExpertiseArea.STATUS_PUBLISHED);
    }

    @Test
    @DisplayName("DTO safety: no admin/lifecycle fields leak into public service responses")
    void dtoSafety() throws Exception {
        stubServicePage(List.of(publishedService()));
        when(serviceItemRepository.findBySlug("alpha-service"))
                .thenReturn(Optional.of(publishedService()));

        String listBody = mockMvc.perform(get(SERVICES))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String detailBody = mockMvc.perform(get(servicePath("alpha-service")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // No lifecycle status, no createdAt bookkeeping, no security noise.
        assertThat(listBody).doesNotContain("\"status\"", "\"createdAt\"", "password", "token");
        assertThat(detailBody).contains("slug", "title", "displayOrder", "updatedAt");
        assertThat(detailBody).doesNotContain("\"status\"", "\"createdAt\"", "password", "token");
    }

    @Test
    @DisplayName("POST on public catalog paths stays authenticated → anonymous gets 401, never a mutation")
    void postOnPublicPathsIs401() throws Exception {
        mockMvc.perform(post(SERVICES).contentType("application/json").content("{\"slug\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(CATEGORIES).contentType("application/json").content("{\"name\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(EXPERTISE).contentType("application/json").content("{\"name\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        verify(serviceItemRepository, org.mockito.Mockito.never()).save(any(ServiceItem.class));
        verify(serviceCategoryRepository, org.mockito.Mockito.never()).save(any(ServiceCategory.class));
        verify(expertiseAreaRepository, org.mockito.Mockito.never()).save(any(ExpertiseArea.class));
    }

    @Test
    @DisplayName("pagination validation: size over the 100 cap → 400 envelope, repository untouched")
    void paginationValidation() throws Exception {
        mockMvc.perform(get(SERVICES).queryParam("size", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        verify(serviceItemRepository, org.mockito.Mockito.never())
                .findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("rate-limit scope: repeated public catalog GETs are NOT rate limited (outside the I-6 limiter, like /jobs)")
    void rateLimitScopeUnchanged() throws Exception {
        stubServicePage(List.of());
        for (int i = 0; i < 15; i++) {
            mockMvc.perform(get(SERVICES)).andExpect(status().isOk());
        }
        verify(serviceItemRepository, org.mockito.Mockito.atLeast(15))
                .findAll(any(Specification.class), any(Pageable.class));
    }
}
