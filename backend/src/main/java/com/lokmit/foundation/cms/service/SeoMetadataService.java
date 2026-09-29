package com.lokmit.foundation.cms.service;

import com.lokmit.foundation.cms.dto.SeoMetadataResponse;
import com.lokmit.foundation.cms.dto.SeoMetadataUpdateRequest;
import com.lokmit.foundation.cms.dto.SeoMetadataUpsertRequest;
import com.lokmit.foundation.cms.entity.SeoMetadata;
import com.lokmit.foundation.cms.repository.SeoMetadataRepository;
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

/**
 * Administrative SEO-metadata management (A4).
 *
 * <p>One record per (entityType, entityId) — the polymorphic pair documented
 * in docs/DATABASE.md D5. Duplicates are pre-checked for a clean 409 and
 * backstopped by the uq_seo_metadata_entity constraint against concurrent
 * creation races. No validation is attempted against the referenced entity:
 * the pair is deliberately FK-less, so creating SEO metadata for an entity
 * that does not (yet) exist is permitted by design and re-pointing a record
 * to a missing entity is not treated as an error here.</p>
 */
@Service
public class SeoMetadataService {

    private static final Logger LOG = LoggerFactory.getLogger(SeoMetadataService.class);

    private final SeoMetadataRepository seoMetadataRepository;

    public SeoMetadataService(SeoMetadataRepository seoMetadataRepository) {
        this.seoMetadataRepository = seoMetadataRepository;
    }

    /**
     * Paginated list, newest first. Optional filters: exact entityType,
     * exact entityId, and case-insensitive contains on SEO title.
     */
    @Transactional(readOnly = true)
    public Page<SeoMetadataResponse> listMetadata(String entityType, Long entityId,
                                                  String search, Pageable pageable) {
        Specification<SeoMetadata> spec = Specification.where(null);
        if (entityType != null && !entityType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entityType"), entityType.trim()));
        }
        if (entityId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entityId"), entityId));
        }
        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("seoTitle")), like));
        }
        return seoMetadataRepository.findAll(spec, pageable).map(SeoMetadataService::toResponse);
    }

    /** Fetch by numeric id. */
    @Transactional(readOnly = true)
    public SeoMetadataResponse getMetadata(long id) {
        return seoMetadataRepository.findById(id)
                .map(SeoMetadataService::toResponse)
                .orElseThrow(() -> new NotFoundException("SEO metadata not found"));
    }

    /** Fetch by the unique (entityType, entityId) pair. */
    @Transactional(readOnly = true)
    public SeoMetadataResponse getMetadataByEntity(String entityType, long entityId) {
        return seoMetadataRepository.findByEntityTypeAndEntityId(entityType, entityId)
                .map(SeoMetadataService::toResponse)
                .orElseThrow(() -> new NotFoundException("SEO metadata not found"));
    }

    /**
     * Creates an SEO record. Duplicate (entityType, entityId) is pre-checked
     * for a clean 409 and backstopped by the unique constraint.
     */
    @Transactional
    public SeoMetadataResponse createMetadata(SeoMetadataUpsertRequest request) {
        if (seoMetadataRepository.existsByEntityTypeAndEntityId(
                request.getEntityType(), request.getEntityId())) {
            throw new ConflictException("SEO metadata already exists for entity '"
                    + request.getEntityType() + "' with id " + request.getEntityId());
        }

        SeoMetadata metadata = new SeoMetadata();
        metadata.setEntityType(request.getEntityType().trim());
        metadata.setEntityId(request.getEntityId());
        metadata.setSeoTitle(request.getSeoTitle());
        metadata.setSeoDescription(request.getSeoDescription());
        metadata.setCanonicalUrl(request.getCanonicalUrl());
        metadata.setOgTitle(request.getOgTitle());
        metadata.setOgDescription(request.getOgDescription());
        metadata.setOgImageUrl(request.getOgImageUrl());
        metadata.setCreatedAt(OffsetDateTime.now());
        metadata.setUpdatedAt(OffsetDateTime.now());

        try {
            return toResponse(seoMetadataRepository.save(metadata));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("SEO metadata already exists for entity '"
                    + request.getEntityType() + "' with id " + request.getEntityId());
        }
    }

    /**
     * Partial update. The identity pair never moves; each provided field is
     * applied, allowing explicit nulls to clear optional content fields.
     */
    @Transactional
    public SeoMetadataResponse updateMetadata(long id, SeoMetadataUpdateRequest request) {
        SeoMetadata metadata = seoMetadataRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SEO metadata not found"));

        if (request.isSeoTitleProvided()) {
            metadata.setSeoTitle(request.getSeoTitle());
        }
        if (request.isSeoDescriptionProvided()) {
            metadata.setSeoDescription(request.getSeoDescription());
        }
        if (request.isCanonicalUrlProvided()) {
            metadata.setCanonicalUrl(request.getCanonicalUrl());
        }
        if (request.isOgTitleProvided()) {
            metadata.setOgTitle(request.getOgTitle());
        }
        if (request.isOgDescriptionProvided()) {
            metadata.setOgDescription(request.getOgDescription());
        }
        if (request.isOgImageUrlProvided()) {
            metadata.setOgImageUrl(request.getOgImageUrl());
        }
        metadata.setUpdatedAt(OffsetDateTime.now());
        return toResponse(seoMetadataRepository.save(metadata));
    }

    static SeoMetadataResponse toResponse(SeoMetadata metadata) {
        return SeoMetadataResponse.builder()
                .id(metadata.getId())
                .entityType(metadata.getEntityType())
                .entityId(metadata.getEntityId())
                .seoTitle(metadata.getSeoTitle())
                .seoDescription(metadata.getSeoDescription())
                .canonicalUrl(metadata.getCanonicalUrl())
                .ogTitle(metadata.getOgTitle())
                .ogDescription(metadata.getOgDescription())
                .ogImageUrl(metadata.getOgImageUrl())
                .createdAt(metadata.getCreatedAt())
                .updatedAt(metadata.getUpdatedAt())
                .build();
    }
}
