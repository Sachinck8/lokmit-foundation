package com.lokmit.foundation.services.catalog.service;

import com.lokmit.foundation.common.exception.BadRequestException;
import com.lokmit.foundation.common.exception.ConflictException;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.services.catalog.dto.ServiceCategoryCreateRequest;
import com.lokmit.foundation.services.catalog.dto.ServiceCategoryResponse;
import com.lokmit.foundation.services.catalog.dto.ServiceCategoryUpdateRequest;
import com.lokmit.foundation.services.catalog.entity.ServiceCategory;
import com.lokmit.foundation.services.catalog.repository.ServiceCategoryRepository;
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
 * Business rules for service-category management (A5).
 *
 * <p>The category catalog is simple by schema design: two unique business
 * keys (name, slug), an ACTIVE/INACTIVE lifecycle and a display order.
 * Slugs are immutable once created — the category is identified by id in the
 * API and the slug is the URL identity. Categories are soft-usable: deleting
 * one does not delete services, the existing FK detaches them (ON DELETE SET
 * NULL), so deletion is safe against the schema's intended detach behavior.
 * No cascades exist and none are introduced.</p>
 */
@Service
public class ServiceCategoryService {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceCategoryService.class);

    public static final Set<String> VALID_STATUSES =
            Set.of(ServiceCategory.STATUS_ACTIVE, ServiceCategory.STATUS_INACTIVE);

    private final ServiceCategoryRepository serviceCategoryRepository;

    public ServiceCategoryService(ServiceCategoryRepository serviceCategoryRepository) {
        this.serviceCategoryRepository = serviceCategoryRepository;
    }

    /**
     * Paginated list, lowest display order first (the catalog's natural
     * ordering), then name. Optional exact status filter, validated against
     * the chk_service_categories_status domain.
     */
    @Transactional(readOnly = true)
    public Page<ServiceCategoryResponse> listCategories(String status, Pageable pageable) {
        Specification<ServiceCategory> spec = Specification.where(null);
        if (status != null && !status.isBlank()) {
            String normalized = status.trim().toUpperCase(Locale.ROOT);
            if (!VALID_STATUSES.contains(normalized)) {
                throw new BadRequestException("Status must be one of: ACTIVE, INACTIVE");
            }
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), normalized));
        }
        return serviceCategoryRepository.findAll(spec, pageable)
                .map(ServiceCategoryService::toResponse);
    }

    /** Fetch by numeric id. */
    @Transactional(readOnly = true)
    public ServiceCategoryResponse getCategory(long id) {
        return serviceCategoryRepository.findById(id)
                .map(ServiceCategoryService::toResponse)
                .orElseThrow(() -> new NotFoundException("Service category not found"));
    }

    /**
     * Creates a category. Always starts ACTIVE (the schema default). Duplicate
     * name or slug is pre-checked for a clean 409 and backstopped by the
     * unique constraints against concurrent creation races.
     */
    @Transactional
    public ServiceCategoryResponse createCategory(ServiceCategoryCreateRequest request) {
        String name = request.getName().trim();
        String slug = request.getSlug().trim();

        if (serviceCategoryRepository.existsByName(name)) {
            throw new ConflictException("Service category name '" + name + "' already exists");
        }
        if (serviceCategoryRepository.existsBySlug(slug)) {
            throw new ConflictException("Service category slug '" + slug + "' already exists");
        }

        ServiceCategory category = new ServiceCategory();
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(request.getDescription());
        category.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        category.setStatus(ServiceCategory.STATUS_ACTIVE);
        category.setCreatedAt(OffsetDateTime.now());
        category.setUpdatedAt(OffsetDateTime.now());

        try {
            return toResponse(serviceCategoryRepository.save(category));
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ConflictException("Service category name or slug already exists");
        }
    }

    /**
     * Partial edit. Slug never moves; omitted fields stay unchanged; status is
     * validated against the schema domain. Unique collisions are pre-checked
     * (excluding the row itself) and backstopped by the constraints.
     */
    @Transactional
    public ServiceCategoryResponse updateCategory(long id, ServiceCategoryUpdateRequest request) {
        ServiceCategory category = serviceCategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Service category not found"));

        if (request.getName() != null) {
            String name = request.getName().trim();
            if (serviceCategoryRepository.existsByNameAndIdNot(name, id)) {
                throw new ConflictException("Service category name '" + name + "' already exists");
            }
            category.setName(name);
        }
        if (request.isDescriptionProvided()) {
            category.setDescription(request.getDescription());
        }
        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getStatus() != null) {
            String normalized = request.getStatus().trim().toUpperCase(Locale.ROOT);
            if (!VALID_STATUSES.contains(normalized)) {
                throw new BadRequestException("Status must be one of: ACTIVE, INACTIVE");
            }
            category.setStatus(normalized);
        }
        category.setUpdatedAt(OffsetDateTime.now());
        return toResponse(serviceCategoryRepository.save(category));
    }

    /**
     * Deletes a category. Safe by schema design: services keep existing, the
     * FK detaches them (ON DELETE SET NULL) — no cascade deletes any business
     * data. The delete is logged with the detached service count.
     */
    @Transactional
    public void deleteCategory(long id) {
        ServiceCategory category = serviceCategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Service category not found"));
        serviceCategoryRepository.delete(category);
        LOG.info("Service category id {} ({}) deleted; referencing services become uncategorized",
                id, category.getSlug());
    }

    static ServiceCategoryResponse toResponse(ServiceCategory category) {
        return ServiceCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .displayOrder(category.getDisplayOrder())
                .status(category.getStatus())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
