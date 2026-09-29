package com.lokmit.foundation.services.catalog.service;

import com.lokmit.foundation.common.exception.BadRequestException;
import com.lokmit.foundation.common.exception.ConflictException;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.services.catalog.dto.ExpertiseAreaCreateRequest;
import com.lokmit.foundation.services.catalog.dto.ExpertiseAreaResponse;
import com.lokmit.foundation.services.catalog.dto.ExpertiseAreaUpdateRequest;
import com.lokmit.foundation.services.catalog.entity.ExpertiseArea;
import com.lokmit.foundation.services.catalog.repository.ExpertiseAreaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Set;

/**
 * Business rules for expertise-area management (A5).
 *
 * <p>Lifecycle mirrors the A4 website-content rules, exactly per the shared
 * chk constraint domain: new areas always start DRAFT; PUBLISH is allowed
 * from DRAFT (idempotent re-publish accepted); ARCHIVE is allowed from DRAFT
 * or PUBLISHED and is terminal. A single unique slug identifies an area.</p>
 */
@Service
public class ExpertiseAreaService {

    private static final Logger LOG = LoggerFactory.getLogger(ExpertiseAreaService.class);

    public static final Set<String> VALID_STATUSES =
            Set.of(ExpertiseArea.STATUS_DRAFT, ExpertiseArea.STATUS_PUBLISHED,
                    ExpertiseArea.STATUS_ARCHIVED);

    private final ExpertiseAreaRepository expertiseAreaRepository;

    public ExpertiseAreaService(ExpertiseAreaRepository expertiseAreaRepository) {
        this.expertiseAreaRepository = expertiseAreaRepository;
    }

    /**
     * Paginated list, lowest display order first, then name. Optional exact
     * status filter (validated against the chk domain) and a case-insensitive
     * name search — parameterized through Specifications.
     */
    @Transactional(readOnly = true)
    public Page<ExpertiseAreaResponse> listAreas(String status, String search,
                                                 Pageable pageable) {
        Specification<ExpertiseArea> spec = Specification.where(null);
        if (status != null && !status.isBlank()) {
            String normalized = status.trim().toUpperCase(Locale.ROOT);
            if (!VALID_STATUSES.contains(normalized)) {
                throw new BadRequestException("Status must be one of: DRAFT, PUBLISHED, ARCHIVED");
            }
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), normalized));
        }
        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), like));
        }
        return expertiseAreaRepository.findAll(spec, pageable)
                .map(ExpertiseAreaService::toResponse);
    }

    /** Fetch by numeric id. */
    @Transactional(readOnly = true)
    public ExpertiseAreaResponse getArea(long id) {
        return expertiseAreaRepository.findById(id)
                .map(ExpertiseAreaService::toResponse)
                .orElseThrow(() -> new NotFoundException("Expertise area not found"));
    }

    /**
     * Creates an area. Always starts DRAFT. Duplicate slug is pre-checked for
     * a clean 409 and backstopped by uq_expertise_areas_slug.
     */
    @Transactional
    public ExpertiseAreaResponse createArea(ExpertiseAreaCreateRequest request) {
        String slug = request.getSlug().trim();

        if (expertiseAreaRepository.existsBySlug(slug)) {
            throw new ConflictException("Expertise area slug '" + slug + "' already exists");
        }

        ExpertiseArea area = new ExpertiseArea();
        area.setSlug(slug);
        area.setName(request.getName().trim());
        area.setDescription(request.getDescription());
        area.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        area.setStatus(ExpertiseArea.STATUS_DRAFT);
        area.setCreatedAt(OffsetDateTime.now());
        area.setUpdatedAt(OffsetDateTime.now());

        try {
            return toResponse(expertiseAreaRepository.save(area));
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ConflictException("Expertise area slug '" + slug + "' already exists");
        }
    }

    /**
     * Partial edit. Slug never moves; omitted fields stay unchanged. Status is
     * not editable here — lifecycle transitions use the dedicated endpoints.
     */
    @Transactional
    public ExpertiseAreaResponse updateArea(long id, ExpertiseAreaUpdateRequest request) {
        ExpertiseArea area = expertiseAreaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Expertise area not found"));

        if (request.getName() != null) {
            area.setName(request.getName().trim());
        }
        if (request.isDescriptionProvided()) {
            area.setDescription(request.getDescription());
        }
        if (request.getDisplayOrder() != null) {
            area.setDisplayOrder(request.getDisplayOrder());
        }
        area.setUpdatedAt(OffsetDateTime.now());
        return toResponse(expertiseAreaRepository.save(area));
    }

    /** Publishes a DRAFT (or already-published) area. */
    @Transactional
    public ExpertiseAreaResponse publishArea(long id) {
        return transition(id, ExpertiseArea.STATUS_PUBLISHED);
    }

    /** Archives a DRAFT or PUBLISHED area; archiving is terminal. */
    @Transactional
    public ExpertiseAreaResponse archiveArea(long id) {
        return transition(id, ExpertiseArea.STATUS_ARCHIVED);
    }

    private ExpertiseAreaResponse transition(long id, String targetStatus) {
        ExpertiseArea area = expertiseAreaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Expertise area not found"));

        if (ExpertiseArea.STATUS_ARCHIVED.equals(area.getStatus())) {
            throw new ConflictException("Archived expertise areas cannot change status");
        }
        if (ExpertiseArea.STATUS_PUBLISHED.equals(targetStatus)
                && ExpertiseArea.STATUS_PUBLISHED.equals(area.getStatus())) {
            throw new ConflictException("Expertise area is already published");
        }
        area.setStatus(targetStatus);
        area.setUpdatedAt(OffsetDateTime.now());
        ExpertiseAreaResponse response = toResponse(expertiseAreaRepository.save(area));
        LOG.info("Expertise area id {} ({}) transitioned to {}", id, area.getSlug(), targetStatus);
        return response;
    }

    /**
     * Deletes an expertise area. The expertise_areas table is not referenced
     * by any other table (verified across V4–V12), so deletion is safe and
     * self-contained.
     */
    @Transactional
    public void deleteArea(long id) {
        ExpertiseArea area = expertiseAreaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Expertise area not found"));
        expertiseAreaRepository.delete(area);
        LOG.info("Expertise area id {} ({}) deleted", id, area.getSlug());
    }

    static ExpertiseAreaResponse toResponse(ExpertiseArea area) {
        return ExpertiseAreaResponse.builder()
                .id(area.getId())
                .slug(area.getSlug())
                .name(area.getName())
                .description(area.getDescription())
                .displayOrder(area.getDisplayOrder())
                .status(area.getStatus())
                .createdAt(area.getCreatedAt())
                .updatedAt(area.getUpdatedAt())
                .build();
    }
}
