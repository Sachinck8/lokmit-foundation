package com.lokmit.foundation.services.catalog.service;

import com.lokmit.foundation.common.exception.BadRequestException;
import com.lokmit.foundation.common.exception.ConflictException;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.services.catalog.dto.ServiceItemCreateRequest;
import com.lokmit.foundation.services.catalog.dto.ServiceItemResponse;
import com.lokmit.foundation.services.catalog.dto.ServiceItemUpdateRequest;
import com.lokmit.foundation.services.catalog.entity.ServiceCategory;
import com.lokmit.foundation.services.catalog.entity.ServiceItem;
import com.lokmit.foundation.services.catalog.repository.ServiceCategoryRepository;
import com.lokmit.foundation.services.catalog.repository.ServiceItemRepository;
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
 * Business rules for service management (A5).
 *
 * <p>Lifecycle mirrors the A4 website-content rules, exactly per the shared
 * chk constraint domain: new services always start DRAFT; PUBLISH is allowed
 * from DRAFT (idempotent re-publish accepted); ARCHIVE is allowed from DRAFT
 * or PUBLISHED and is terminal. Referenced categories are validated against
 * the service_categories table — an unknown category id is a 404, never an
 * orphaning insert (the DB would reject it, but the API answers before that).</p>
 */
@Service
public class ServiceItemService {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceItemService.class);

    public static final Set<String> VALID_STATUSES =
            Set.of(ServiceItem.STATUS_DRAFT, ServiceItem.STATUS_PUBLISHED,
                    ServiceItem.STATUS_ARCHIVED);

    private final ServiceItemRepository serviceItemRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;

    public ServiceItemService(ServiceItemRepository serviceItemRepository,
                              ServiceCategoryRepository serviceCategoryRepository) {
        this.serviceItemRepository = serviceItemRepository;
        this.serviceCategoryRepository = serviceCategoryRepository;
    }

    /**
     * Paginated list, lowest display order first, then title. Optional exact
     * filters: categoryId (must be a positive number when present), status
     * (validated against the chk_services_status domain) and a case-insensitive
     * title/summary search — all parameterized through Specifications, so the
     * filtering runs database-side over the existing idx_services_category
     * index.
     */
    @Transactional(readOnly = true)
    public Page<ServiceItemResponse> listServices(Long categoryId, String status,
                                                  String search, Pageable pageable) {
        Specification<ServiceItem> spec = Specification.where(null);
        if (categoryId != null) {
            if (categoryId < 1) {
                throw new BadRequestException("Category id must be a positive number");
            }
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("category").get("id"), categoryId));
        }
        if (status != null && !status.isBlank()) {
            String normalized = status.trim().toUpperCase(Locale.ROOT);
            if (!VALID_STATUSES.contains(normalized)) {
                throw new BadRequestException("Status must be one of: DRAFT, PUBLISHED, ARCHIVED");
            }
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), normalized));
        }
        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("summary")), like)));
        }
        return serviceItemRepository.findAll(spec, pageable)
                .map(ServiceItemService::toResponse);
    }

    /** Fetch by numeric id. */
    @Transactional(readOnly = true)
    public ServiceItemResponse getService(long id) {
        return serviceItemRepository.findById(id)
                .map(ServiceItemService::toResponse)
                .orElseThrow(() -> new NotFoundException("Service not found"));
    }

    /**
     * Creates a service. Always starts DRAFT. Duplicate slug is pre-checked
     * for a clean 409 and backstopped by uq_services_slug. A provided
     * categoryId must reference an existing category (404 otherwise).
     */
    @Transactional
    public ServiceItemResponse createService(ServiceItemCreateRequest request) {
        String slug = request.getSlug().trim();

        if (serviceItemRepository.existsBySlug(slug)) {
            throw new ConflictException("Service slug '" + slug + "' already exists");
        }
        ServiceCategory category = resolveCategory(request.getCategoryId());

        ServiceItem item = new ServiceItem();
        item.setSlug(slug);
        item.setTitle(request.getTitle().trim());
        item.setSummary(request.getSummary());
        item.setDescription(request.getDescription());
        item.setCategory(category);
        item.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        item.setStatus(ServiceItem.STATUS_DRAFT);
        item.setCreatedAt(OffsetDateTime.now());
        item.setUpdatedAt(OffsetDateTime.now());

        try {
            return toResponse(serviceItemRepository.save(item));
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ConflictException("Service slug '" + slug + "' already exists");
        }
    }

    /**
     * Partial edit. Slug never moves; omitted fields stay unchanged;
     * categoryId explicitly set to null detaches the service (uncategorized);
     * a new category id is validated (404 when unknown). Status is not
     * editable here — lifecycle transitions use the dedicated endpoints.
     */
    @Transactional
    public ServiceItemResponse updateService(long id, ServiceItemUpdateRequest request) {
        ServiceItem item = serviceItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Service not found"));

        if (request.getTitle() != null) {
            item.setTitle(request.getTitle().trim());
        }
        if (request.isSummaryProvided()) {
            item.setSummary(request.getSummary());
        }
        if (request.isDescriptionProvided()) {
            item.setDescription(request.getDescription());
        }
        if (request.isCategoryIdProvided()) {
            item.setCategory(resolveCategory(request.getCategoryId()));
        }
        if (request.getDisplayOrder() != null) {
            item.setDisplayOrder(request.getDisplayOrder());
        }
        item.setUpdatedAt(OffsetDateTime.now());
        return toResponse(serviceItemRepository.save(item));
    }

    /** Publishes a DRAFT (or already-published) service. */
    @Transactional
    public ServiceItemResponse publishService(long id) {
        return transition(id, ServiceItem.STATUS_PUBLISHED);
    }

    /** Archives a DRAFT or PUBLISHED service; archiving is terminal. */
    @Transactional
    public ServiceItemResponse archiveService(long id) {
        return transition(id, ServiceItem.STATUS_ARCHIVED);
    }

    private ServiceItemResponse transition(long id, String targetStatus) {
        ServiceItem item = serviceItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Service not found"));

        if (ServiceItem.STATUS_ARCHIVED.equals(item.getStatus())) {
            throw new ConflictException("Archived services cannot change status");
        }
        if (ServiceItem.STATUS_PUBLISHED.equals(targetStatus)
                && ServiceItem.STATUS_PUBLISHED.equals(item.getStatus())) {
            throw new ConflictException("Service is already published");
        }
        item.setStatus(targetStatus);
        item.setUpdatedAt(OffsetDateTime.now());
        ServiceItemResponse response = toResponse(serviceItemRepository.save(item));
        LOG.info("Service id {} ({}) transitioned to {}", id, item.getSlug(), targetStatus);
        return response;
    }

    /**
     * Deletes a service. The services table is not referenced by any other
     * table (verified across V4–V12), so deletion is safe and self-contained.
     */
    @Transactional
    public void deleteService(long id) {
        ServiceItem item = serviceItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Service not found"));
        serviceItemRepository.delete(item);
        LOG.info("Service id {} ({}) deleted", id, item.getSlug());
    }

    /** Resolves and validates a category reference; null stays uncategorized. */
    private ServiceCategory resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return serviceCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Service category " + categoryId + " not found"));
    }

    static ServiceItemResponse toResponse(ServiceItem item) {
        return ServiceItemResponse.builder()
                .id(item.getId())
                .slug(item.getSlug())
                .title(item.getTitle())
                .summary(item.getSummary())
                .description(item.getDescription())
                .category(item.getCategory() != null
                        ? ServiceCategoryService.toResponse(item.getCategory()) : null)
                .displayOrder(item.getDisplayOrder())
                .status(item.getStatus())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
