package com.lokmit.foundation.cms.service;

import com.lokmit.foundation.cms.dto.WebsiteContentCreateRequest;
import com.lokmit.foundation.cms.dto.WebsiteContentResponse;
import com.lokmit.foundation.cms.dto.WebsiteContentUpdateRequest;
import com.lokmit.foundation.cms.entity.WebsiteContent;
import com.lokmit.foundation.cms.repository.WebsiteContentRepository;
import com.lokmit.foundation.common.exception.BadRequestException;
import com.lokmit.foundation.common.exception.ConflictException;
import com.lokmit.foundation.common.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Set;

/**
 * Administrative website-content management (A4).
 *
 * <p>Lifecycle rules (enforced server-side, mirroring the V3 check
 * constraint — no invented workflow):</p>
 * <ul>
 *   <li>New sections always start as DRAFT.</li>
 *   <li>PUBLISH is allowed only from DRAFT (idempotent re-publish of an
 *       already-published section is accepted).</li>
 *   <li>ARCHIVE is allowed from DRAFT or PUBLISHED; archived sections are
 *       terminal and cannot be re-published.</li>
 *   <li>Status transitions and deletion require {@code content:publish};
 *       ordinary edits require {@code content:manage}. This matches the V2
 *       seed: EDITOR drafts, ADMIN/SUPER_ADMIN publish.</li>
 * </ul>
 */
@Service
public class WebsiteContentService {

    private static final Logger LOG = LoggerFactory.getLogger(WebsiteContentService.class);

    public static final Set<String> VALID_STATUSES =
            Set.of(WebsiteContent.STATUS_DRAFT, WebsiteContent.STATUS_PUBLISHED,
                    WebsiteContent.STATUS_ARCHIVED);

    private final WebsiteContentRepository websiteContentRepository;

    public WebsiteContentService(WebsiteContentRepository websiteContentRepository) {
        this.websiteContentRepository = websiteContentRepository;
    }

    /**
     * Paginated list, newest first. Optional exact filters on pageKey,
     * sectionKey and status; pageKey/sectionKey values arrive from validated
     * path/query params and are bound as parameters (Specification).
     */
    @Transactional(readOnly = true)
    public Page<WebsiteContentResponse> listContent(String pageKey, String sectionKey,
                                                    String status, Pageable pageable) {
        Specification<WebsiteContent> spec = Specification.where(null);
        if (pageKey != null && !pageKey.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("pageKey"), pageKey.trim()));
        }
        if (sectionKey != null && !sectionKey.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("sectionKey"), sectionKey.trim()));
        }
        if (status != null && !status.isBlank()) {
            String normalized = status.trim().toUpperCase(Locale.ROOT);
            if (!VALID_STATUSES.contains(normalized)) {
                throw new BadRequestException("Status must be one of: DRAFT, PUBLISHED, ARCHIVED");
            }
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), normalized));
        }
        return websiteContentRepository.findAll(spec, pageable).map(WebsiteContentService::toResponse);
    }

    /** Fetch by numeric id. */
    @Transactional(readOnly = true)
    public WebsiteContentResponse getContent(long id) {
        return websiteContentRepository.findById(id)
                .map(WebsiteContentService::toResponse)
                .orElseThrow(() -> new NotFoundException("Website content not found"));
    }

    /** Fetch by the composite unique (pageKey, sectionKey) pair. */
    @Transactional(readOnly = true)
    public WebsiteContentResponse getContentByKey(String pageKey, String sectionKey) {
        return websiteContentRepository.findByPageKeyAndSectionKey(pageKey, sectionKey)
                .map(WebsiteContentService::toResponse)
                .orElseThrow(() -> new NotFoundException("Website content not found"));
    }

    /**
     * Creates a section. Always starts as DRAFT; duplicate (pageKey,
     * sectionKey) is pre-checked for a clean 409 and backstopped by the
     * unique constraint against concurrent creation races.
     */
    @Transactional
    public WebsiteContentResponse createContent(WebsiteContentCreateRequest request) {
        String pageKey = request.getPageKey().trim();
        String sectionKey = request.getSectionKey().trim();

        if (request.getContentJson() != null) {
            CmsJsonValidator.requireWellFormedJson(request.getContentJson(), "contentJson");
        }

        if (websiteContentRepository.existsByPageKeyAndSectionKey(pageKey, sectionKey)) {
            throw new ConflictException(
                    "Content section already exists for page '" + pageKey + "' and section '" + sectionKey + "'");
        }

        WebsiteContent content = new WebsiteContent();
        content.setPageKey(pageKey);
        content.setSectionKey(sectionKey);
        content.setTitle(request.getTitle());
        content.setContentJson(request.getContentJson());
        content.setStatus(WebsiteContent.STATUS_DRAFT);
        content.setCreatedAt(OffsetDateTime.now());
        content.setUpdatedAt(OffsetDateTime.now());

        try {
            return toResponse(websiteContentRepository.save(content));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(
                    "Content section already exists for page '" + pageKey + "' and section '" + sectionKey + "'");
        }
    }

    /**
     * Partial edit of title/body. Status is intentionally not editable here;
     * identity keys never move. Publishing/archiving require the dedicated
     * lifecycle endpoints.
     */
    @Transactional
    public WebsiteContentResponse updateContent(long id, WebsiteContentUpdateRequest request) {
        WebsiteContent content = websiteContentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Website content not found"));

        if (request.isContentJsonProvided() && request.getContentJson() != null) {
            CmsJsonValidator.requireWellFormedJson(request.getContentJson(), "contentJson");
        }
        if (request.isTitleProvided()) {
            content.setTitle(request.getTitle());
        }
        if (request.isContentJsonProvided()) {
            content.setContentJson(request.getContentJson());
        }
        content.setUpdatedAt(OffsetDateTime.now());
        return toResponse(websiteContentRepository.save(content));
    }

    /**
     * Publishes a DRAFT (or already-published) section. Requires
     * content:publish at the controller.
     */
    @Transactional
    public WebsiteContentResponse publishContent(long id) {
        return transition(id, WebsiteContent.STATUS_PUBLISHED, true);
    }

    /**
     * Archives a DRAFT or PUBLISHED section. Requires content:publish at the
     * controller.
     */
    @Transactional
    public WebsiteContentResponse archiveContent(long id) {
        return transition(id, WebsiteContent.STATUS_ARCHIVED, true);
    }

    private WebsiteContentResponse transition(long id, String targetStatus,
                                              boolean requireNotArchivedSource) {
        WebsiteContent content = websiteContentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Website content not found"));

        if (WebsiteContent.STATUS_ARCHIVED.equals(targetStatus)
                && WebsiteContent.STATUS_ARCHIVED.equals(content.getStatus())) {
            throw new ConflictException("Content section is already archived");
        }
        if (WebsiteContent.STATUS_PUBLISHED.equals(targetStatus)
                && WebsiteContent.STATUS_ARCHIVED.equals(content.getStatus())) {
            throw new ConflictException("Archived content cannot be re-published");
        }

        content.setStatus(targetStatus);
        content.setUpdatedAt(OffsetDateTime.now());
        WebsiteContentResponse response = toResponse(websiteContentRepository.save(content));
        LOG.info("Website content id {} transitioned to {}", id, targetStatus);
        return response;
    }

    /**
     * Deletes a section. Allowed from any status (archived content is the
     * intended cleanup target), but the operation is guarded by
     * content:publish so only administrators with publishing authority can
     * remove published content.
     */
    @Transactional
    public void deleteContent(long id) {
        WebsiteContent content = websiteContentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Website content not found"));
        websiteContentRepository.delete(content);
        LOG.info("Website content id {} ({} / {}) deleted", id, content.getPageKey(), content.getSectionKey());
    }

    static WebsiteContentResponse toResponse(WebsiteContent content) {
        return WebsiteContentResponse.builder()
                .id(content.getId())
                .pageKey(content.getPageKey())
                .sectionKey(content.getSectionKey())
                .title(content.getTitle())
                .contentJson(content.getContentJson())
                .status(content.getStatus())
                .createdAt(content.getCreatedAt())
                .updatedAt(content.getUpdatedAt())
                .build();
    }
}
