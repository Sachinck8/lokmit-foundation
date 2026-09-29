package com.lokmit.foundation.cms.controller;

import com.lokmit.foundation.cms.dto.SiteSettingResponse;
import com.lokmit.foundation.cms.dto.WebsiteContentResponse;
import com.lokmit.foundation.cms.entity.SeoMetadata;
import com.lokmit.foundation.cms.entity.SiteSetting;
import com.lokmit.foundation.cms.entity.WebsiteContent;
import com.lokmit.foundation.cms.repository.SeoMetadataRepository;
import com.lokmit.foundation.cms.repository.SiteSettingRepository;
import com.lokmit.foundation.cms.repository.WebsiteContentRepository;
import com.lokmit.foundation.cms.service.SeoMetadataService;
import com.lokmit.foundation.cms.service.SiteSettingService;
import com.lokmit.foundation.cms.service.WebsiteContentService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.springframework.test.web.servlet.ResultActions;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Admin CMS security and behavior matrix (A4), exercised through the REAL
 * Spring Security filter chain (real JwtAuthenticationFilter, real
 * JwtTokenProvider, real CustomUserDetailsService with mocked repositories —
 * identical to the A1/A2/A3 matrix setups).
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>anonymous → 401 on every CMS endpoint</li>
 *   <li>permission-tier matrix: EDITOR (content:manage only) 200 on reads /
 *       edits, 403 on publish/archive/delete; CANDIDATE 403 everywhere;
 *       MODERATOR 403 on settings</li>
 *   <li>SUPER_ADMIN and ADMIN → 200 on permitted operations</li>
 *   <li>JWT roles-claim spoofing does not elevate</li>
 *   <li>validation failures → 400; missing resources → 404;
 *       duplicates → 409</li>
 *   <li>pagination + filtering behavior</li>
 *   <li>DTO safety: no security-sensitive fields</li>
 * </ul>
 */
@WebMvcTest(controllers = {AdminSiteSettingController.class,
        AdminWebsiteContentController.class, AdminSeoMetadataController.class})
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        JwtTokenProvider.class, CustomUserDetailsService.class, SecurityUtils.class,
        SiteSettingService.class, WebsiteContentService.class, SeoMetadataService.class})
@AutoConfigureMockMvc
class AdminCmsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SiteSettingRepository siteSettingRepository;

    @MockitoBean
    private WebsiteContentRepository websiteContentRepository;

    @MockitoBean
    private SeoMetadataRepository seoMetadataRepository;

    @MockitoBean
    private UserRepository userRepository;

    private static final String SETTINGS = ApiPaths.ADMIN_CMS_SITE_SETTINGS;
    private static final String CONTENT = ApiPaths.ADMIN_CMS_WEBSITE_CONTENT;
    private static final String SEO = ApiPaths.ADMIN_CMS_SEO_METADATA;

    private static final String SUPER_ADMIN_EMAIL = "superadmin@lokmitfoundation.org";
    private static final String ADMIN_EMAIL = "admin@lokmitfoundation.org";
    private static final String EDITOR_EMAIL = "editor@lokmitfoundation.org";
    private static final String MODERATOR_EMAIL = "moderator@lokmitfoundation.org";
    private static final String CANDIDATE_EMAIL = "candidate@lokmitfoundation.org";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void resetStubs() {
        org.mockito.Mockito.reset(siteSettingRepository, websiteContentRepository,
                seoMetadataRepository, userRepository);
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

    private void stubEmptyPage(SiteSettingRepository repo) {
        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
    }

    private void stubEmptyPage(WebsiteContentRepository repo) {
        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
    }

    private void stubEmptyPage(SeoMetadataRepository repo) {
        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
    }

    private void stubContentSaveEcho() {
        when(websiteContentRepository.save(any(WebsiteContent.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private String tokenFor(long id, String email, List<String> claimRoles) {
        return jwtTokenProvider.generateAccessToken(id, email, claimRoles);
    }

    private SiteSetting dbSetting() {
        SiteSetting setting = new SiteSetting();
        setting.setId(1L);
        setting.setSettingKey("site.contact_email");
        setting.setSettingValue("info@lokmitfoundation.org");
        setting.setDescription("Public contact address");
        setting.setCreatedAt(OffsetDateTime.now());
        setting.setUpdatedAt(OffsetDateTime.now());
        return setting;
    }

    private WebsiteContent dbContent() {
        WebsiteContent content = new WebsiteContent();
        content.setId(7L);
        content.setPageKey("home");
        content.setSectionKey("hero");
        content.setTitle("Hero section");
        content.setContentJson("{\"headline\":\"Welcome\"}");
        content.setStatus("DRAFT");
        content.setCreatedAt(OffsetDateTime.now());
        content.setUpdatedAt(OffsetDateTime.now());
        return content;
    }

    private SeoMetadata dbSeo() {
        SeoMetadata seo = new SeoMetadata();
        seo.setId(3L);
        seo.setEntityType("website_content");
        seo.setEntityId(7L);
        seo.setSeoTitle("Home page");
        seo.setCreatedAt(OffsetDateTime.now());
        seo.setUpdatedAt(OffsetDateTime.now());
        return seo;
    }

    // ------------------------------------------------------------------
    // A. Anonymous → 401 (one probe per controller + write probes)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("anonymous access is rejected with 401 on all CMS namespaces")
    void anonymousIs401Everywhere() throws Exception {
        mockMvc.perform(get(SETTINGS)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(CONTENT)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(SEO)).andExpect(status().isUnauthorized());

        mockMvc.perform(patch(SETTINGS + "/1")
                        .contentType("application/json").content("{\"settingValue\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(CONTENT)
                        .contentType("application/json").content("{\"pageKey\":\"p\",\"sectionKey\":\"s\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(CONTENT + "/1/publish"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(CONTENT + "/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(SEO)
                        .contentType("application/json")
                        .content("{\"entityType\":\"page\",\"entityId\":1}"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // B. Permission tiers
    // ------------------------------------------------------------------

    @Test
    @DisplayName("EDITOR (content:manage, no content:publish) reads/edits content but cannot publish/archive/delete")
    void editorTierBoundaries() throws Exception {
        givenDbUser(dbUser(5L, EDITOR_EMAIL, role("EDITOR", "content:manage", "downloads:manage")));
        String token = tokenFor(5L, EDITOR_EMAIL, List.of("EDITOR"));

        when(websiteContentRepository.findById(7L)).thenReturn(Optional.of(dbContent()));
        stubEmptyPage(websiteContentRepository);
        stubContentSaveEcho();

        // Reads and edits allowed.
        mockMvc.perform(get(CONTENT).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get(CONTENT + "/7").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(patch(CONTENT + "/7").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"title\":\"New title\"}"))
                .andExpect(status().isOk());

        // Lifecycle/deletion denied.
        mockMvc.perform(post(CONTENT + "/7/publish").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(CONTENT + "/7/archive").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(CONTENT + "/7").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        verify(websiteContentRepository, never()).delete(any(WebsiteContent.class));
    }

    @Test
    @DisplayName("CANDIDATE (no permissions) gets 403 on every CMS namespace")
    void candidateIs403Everywhere() throws Exception {
        givenDbUser(dbUser(2L, CANDIDATE_EMAIL, role("CANDIDATE")));
        String token = tokenFor(2L, CANDIDATE_EMAIL, List.of("CANDIDATE"));

        mockMvc.perform(get(SETTINGS).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(CONTENT).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(SEO).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("MODERATOR (no content/settings permissions) gets 403 on settings")
    void moderatorIs403OnSettings() throws Exception {
        givenDbUser(dbUser(3L, MODERATOR_EMAIL, role("MODERATOR", "jobs:moderate", "messages:manage")));
        String token = tokenFor(3L, MODERATOR_EMAIL, List.of("MODERATOR"));

        mockMvc.perform(get(SETTINGS).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN (settings:manage + content:manage + content:publish) has full CMS access")
    void adminHasFullAccess() throws Exception {
        givenDbUser(dbUser(4L, ADMIN_EMAIL, role("ADMIN", "content:manage", "content:publish",
                "downloads:manage", "messages:manage", "jobs:manage", "settings:manage",
                "dashboard:view")));
        String token = tokenFor(4L, ADMIN_EMAIL, List.of("ADMIN"));

        when(websiteContentRepository.findById(7L)).thenReturn(Optional.of(dbContent()));
        stubEmptyPage(websiteContentRepository);
        stubContentSaveEcho();

        mockMvc.perform(get(CONTENT).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(post(CONTENT + "/7/publish").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("SUPER_ADMIN has full CMS access")
    void superAdminHasFullAccess() throws Exception {
        givenDbUser(dbUser(1L, SUPER_ADMIN_EMAIL, role("SUPER_ADMIN", "content:manage",
                "content:publish", "downloads:manage", "messages:manage", "users:manage",
                "jobs:manage", "jobs:moderate", "settings:manage", "dashboard:view")));
        String token = tokenFor(1L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        stubEmptyPage(siteSettingRepository);
        stubEmptyPage(websiteContentRepository);
        stubEmptyPage(seoMetadataRepository);

        mockMvc.perform(get(SETTINGS).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get(SEO).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------
    // C. JWT claim spoofing gains nothing
    // ------------------------------------------------------------------

    @Test
    @DisplayName("spoofed ADMIN role claim does not grant settings:manage")
    void spoofedClaimIs403() throws Exception {
        givenDbUser(dbUser(9L, CANDIDATE_EMAIL, role("CANDIDATE")));
        String token = tokenFor(9L, CANDIDATE_EMAIL, List.of("ADMIN", "SUPER_ADMIN"));

        mockMvc.perform(get(SETTINGS).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // D. Site settings behavior
    // ------------------------------------------------------------------

    @Test
    @DisplayName("settings list is paginated and key-search filters database-side")
    void settingsListAndSearch() throws Exception {
        givenDbUser(dbUser(4L, ADMIN_EMAIL, role("ADMIN", "settings:manage", "content:manage",
                "content:publish", "downloads:manage", "messages:manage", "jobs:manage",
                "dashboard:view")));
        String token = tokenFor(4L, ADMIN_EMAIL, List.of("ADMIN"));

        when(siteSettingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dbSetting()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get(SETTINGS).header("Authorization", "Bearer " + token)
                        .param("search", "contact").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].settingKey").value("site.contact_email"))
                .andExpect(jsonPath("$.data.totalItems").value(1));

        ArgumentCaptor<Specification<SiteSetting>> specCaptor =
                ArgumentCaptor.forClass(Specification.class);
        verify(siteSettingRepository).findAll(specCaptor.capture(), any(Pageable.class));
        assertThat(specCaptor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("setting update applies only provided fields; omitted stay unchanged")
    void settingPartialUpdate() throws Exception {
        givenDbUser(dbUser(4L, ADMIN_EMAIL, role("ADMIN", "settings:manage", "content:manage",
                "content:publish", "downloads:manage", "messages:manage", "jobs:manage",
                "dashboard:view")));
        String token = tokenFor(4L, ADMIN_EMAIL, List.of("ADMIN"));

        SiteSetting setting = dbSetting();
        when(siteSettingRepository.findById(1L)).thenReturn(Optional.of(setting));
        when(siteSettingRepository.save(any(SiteSetting.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(patch(SETTINGS + "/1").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"description\":\"Updated note\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settingValue").value("info@lokmitfoundation.org"))
                .andExpect(jsonPath("$.data.description").value("Updated note"));

        assertThat(setting.getSettingValue()).isEqualTo("info@lokmitfoundation.org");
    }

    @Test
    @DisplayName("setting lookup by unique key works; unknown key returns 404")
    void settingByKeyAnd404() throws Exception {
        givenDbUser(dbUser(4L, ADMIN_EMAIL, role("ADMIN", "settings:manage", "content:manage",
                "content:publish", "downloads:manage", "messages:manage", "jobs:manage",
                "dashboard:view")));
        String token = tokenFor(4L, ADMIN_EMAIL, List.of("ADMIN"));

        when(siteSettingRepository.findBySettingKey("site.contact_email"))
                .thenReturn(Optional.of(dbSetting()));
        mockMvc.perform(get(SETTINGS + "/key/site.contact_email")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settingKey").value("site.contact_email"));

        when(siteSettingRepository.findBySettingKey("missing.key")).thenReturn(Optional.empty());
        mockMvc.perform(get(SETTINGS + "/key/missing.key")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("setting value cannot be set to null (400)")
    void settingNullValueRejected() throws Exception {
        givenDbUser(dbUser(4L, ADMIN_EMAIL, role("ADMIN", "settings:manage", "content:manage",
                "content:publish", "downloads:manage", "messages:manage", "jobs:manage",
                "dashboard:view")));
        String token = tokenFor(4L, ADMIN_EMAIL, List.of("ADMIN"));

        when(siteSettingRepository.findById(1L)).thenReturn(Optional.of(dbSetting()));

        mockMvc.perform(patch(SETTINGS + "/1").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"settingValue\":null}"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // E. Website content behavior: create, JSON validation, 404, 409
    // ------------------------------------------------------------------

    @Test
    @DisplayName("content creation starts as DRAFT and duplicate pairs get 409")
    void contentCreateAndDuplicate409() throws Exception {
        givenDbUser(dbUser(5L, EDITOR_EMAIL, role("EDITOR", "content:manage", "downloads:manage")));
        String token = tokenFor(5L, EDITOR_EMAIL, List.of("EDITOR"));

        when(websiteContentRepository.existsByPageKeyAndSectionKey("about", "story"))
                .thenReturn(false);
        when(websiteContentRepository.save(any(WebsiteContent.class)))
                .thenAnswer(inv -> {
                    WebsiteContent c = inv.getArgument(0);
                    c.setId(11L);
                    return c;
                });

        mockMvc.perform(post(CONTENT).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"pageKey\":\"about\",\"sectionKey\":\"story\","
                                + "\"title\":\"Our story\",\"contentJson\":\"{\\\"body\\\":\\\"Hi\\\"}\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        // Duplicate pre-check → 409
        when(websiteContentRepository.existsByPageKeyAndSectionKey("about", "story"))
                .thenReturn(true);
        mockMvc.perform(post(CONTENT).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"pageKey\":\"about\",\"sectionKey\":\"story\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("malformed contentJson is rejected with 400")
    void malformedJsonIs400() throws Exception {
        givenDbUser(dbUser(5L, EDITOR_EMAIL, role("EDITOR", "content:manage", "downloads:manage")));
        String token = tokenFor(5L, EDITOR_EMAIL, List.of("EDITOR"));

        mockMvc.perform(post(CONTENT).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"pageKey\":\"about\",\"sectionKey\":\"story2\","
                                + "\"contentJson\":\"{not-json}\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("unknown content id returns 404; archived content cannot be re-published (409)")
    void content404AndArchiveRules() throws Exception {
        givenDbUser(dbUser(4L, ADMIN_EMAIL, role("ADMIN", "settings:manage", "content:manage",
                "content:publish", "downloads:manage", "messages:manage", "jobs:manage",
                "dashboard:view")));
        String token = tokenFor(4L, ADMIN_EMAIL, List.of("ADMIN"));

        when(websiteContentRepository.findById(404L)).thenReturn(Optional.empty());
        mockMvc.perform(get(CONTENT + "/404").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        WebsiteContent archived = dbContent();
        archived.setStatus("ARCHIVED");
        when(websiteContentRepository.findById(7L)).thenReturn(Optional.of(archived));

        mockMvc.perform(post(CONTENT + "/7/publish").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("invalid status filter value returns 400")
    void invalidStatusFilter400() throws Exception {
        givenDbUser(dbUser(5L, EDITOR_EMAIL, role("EDITOR", "content:manage", "downloads:manage")));
        String token = tokenFor(5L, EDITOR_EMAIL, List.of("EDITOR"));

        mockMvc.perform(get(CONTENT).header("Authorization", "Bearer " + token)
                        .param("status", "PENDING"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // F. SEO metadata behavior: create, duplicate 409, by-entity read
    // ------------------------------------------------------------------

    @Test
    @DisplayName("SEO creation returns 201; duplicate entity pair gets 409; lookup by entity works")
    void seoCreateDuplicateAndLookup() throws Exception {
        givenDbUser(dbUser(5L, EDITOR_EMAIL, role("EDITOR", "content:manage", "downloads:manage")));
        String token = tokenFor(5L, EDITOR_EMAIL, List.of("EDITOR"));

        when(seoMetadataRepository.existsByEntityTypeAndEntityId("website_content", 7L))
                .thenReturn(false);
        when(seoMetadataRepository.save(any(SeoMetadata.class)))
                .thenAnswer(inv -> {
                    SeoMetadata s = inv.getArgument(0);
                    s.setId(21L);
                    return s;
                });

        mockMvc.perform(post(SEO).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"entityType\":\"website_content\",\"entityId\":7,"
                                + "\"seoTitle\":\"Home page\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.entityType").value("website_content"));

        when(seoMetadataRepository.existsByEntityTypeAndEntityId("website_content", 7L))
                .thenReturn(true);
        mockMvc.perform(post(SEO).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"entityType\":\"website_content\",\"entityId\":7}"))
                .andExpect(status().isConflict());

        when(seoMetadataRepository.findByEntityTypeAndEntityId("website_content", 7L))
                .thenReturn(Optional.of(dbSeo()));
        mockMvc.perform(get(SEO + "/entity/website_content/7")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entityId").value(7));
    }

    @Test
    @DisplayName("SEO validation failures return 400 envelope")
    void seoValidationFailures() throws Exception {
        givenDbUser(dbUser(5L, EDITOR_EMAIL, role("EDITOR", "content:manage", "downloads:manage")));
        String token = tokenFor(5L, EDITOR_EMAIL, List.of("EDITOR"));

        // Missing required entity fields.
        mockMvc.perform(post(SEO).header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"seoTitle\":\"x\"}"))
                .andExpect(status().isBadRequest());

        // Invalid entity-type pattern.
        mockMvc.perform(post(SEO).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"entityType\":\"Bad Type!\",\"entityId\":1}"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // G. DTO safety
    // ------------------------------------------------------------------

    @Test
    @DisplayName("CMS responses never contain security-sensitive fields")
    void noSensitiveFieldsLeak() throws Exception {
        givenDbUser(dbUser(1L, SUPER_ADMIN_EMAIL, role("SUPER_ADMIN", "content:manage",
                "content:publish", "downloads:manage", "messages:manage", "users:manage",
                "jobs:manage", "jobs:moderate", "settings:manage", "dashboard:view")));
        String token = tokenFor(1L, SUPER_ADMIN_EMAIL, List.of("SUPER_ADMIN"));

        when(siteSettingRepository.findById(1L)).thenReturn(Optional.of(dbSetting()));
        when(websiteContentRepository.findById(7L)).thenReturn(Optional.of(dbContent()));
        when(seoMetadataRepository.findById(3L)).thenReturn(Optional.of(dbSeo()));

        String settingsBody = mockMvc.perform(get(SETTINGS + "/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String contentBody = mockMvc.perform(get(CONTENT + "/7")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String seoBody = mockMvc.perform(get(SEO + "/3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        for (String body : List.of(settingsBody, contentBody, seoBody)) {
            assertThat(body)
                    .doesNotContain("passwordHash", "password_hash", "tokenHash",
                            "refreshToken", "failedLoginAttempts", "lockedUntil");
        }
    }
}
