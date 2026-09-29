package com.lokmit.foundation.services.catalog.service;

import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.services.catalog.dto.PublicExpertiseAreaResponse;
import com.lokmit.foundation.services.catalog.dto.PublicServiceCategoryResponse;
import com.lokmit.foundation.services.catalog.dto.PublicServiceItemResponse;
import com.lokmit.foundation.services.catalog.entity.ExpertiseArea;
import com.lokmit.foundation.services.catalog.entity.ServiceCategory;
import com.lokmit.foundation.services.catalog.entity.ServiceItem;
import com.lokmit.foundation.services.catalog.repository.ExpertiseAreaRepository;
import com.lokmit.foundation.services.catalog.repository.ServiceCategoryRepository;
import com.lokmit.foundation.services.catalog.repository.ServiceItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Public (anonymous) services browsing (A26) over the existing V- tables.
 *
 * <p>Visibility rules, mirroring the A8 public-jobs convention:</p>
 * <ul>
 *   <li>service items and expertise areas: only {@code status = PUBLISHED}</li>
 *   <li>service categories: only {@code status = ACTIVE}</li>
 *   <li>a service item whose category is INACTIVE is not public (its
 *       category data must never leak through a published item)</li>
 * </ul>
 *
 * <p>Unknown slug AND known-but-not-published slug both produce the identical
 * {@link NotFoundException} — the endpoint never reveals whether an
 * unpublished record exists.</p>
 */
@Service
public class PublicCatalogService {

    private final ServiceItemRepository serviceItemRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final ExpertiseAreaRepository expertiseAreaRepository;

    public PublicCatalogService(ServiceItemRepository serviceItemRepository,
                                ServiceCategoryRepository serviceCategoryRepository,
                                ExpertiseAreaRepository expertiseAreaRepository) {
        this.serviceItemRepository = serviceItemRepository;
        this.serviceCategoryRepository = serviceCategoryRepository;
        this.expertiseAreaRepository = expertiseAreaRepository;
    }

    /** Lists published services (newest-published first, then display order). */
    @Transactional(readOnly = true)
    public Page<PublicServiceItemResponse> listPublishedServices(Pageable pageable) {
        Specification<ServiceItem> spec = (root, query, cb) ->
                cb.equal(root.get("status"), ServiceItem.STATUS_PUBLISHED);
        return serviceItemRepository.findAll(spec, pageable).map(PublicCatalogService::toResponse);
    }

    /**
     * Fetches one published service by slug. Unknown slug and
     * known-but-not-published slug are indistinguishable (404).
     */
    @Transactional(readOnly = true)
    public PublicServiceItemResponse getPublishedService(String slug) {
        ServiceItem item = serviceItemRepository.findBySlug(slug)
                .filter(i -> ServiceItem.STATUS_PUBLISHED.equals(i.getStatus()))
                .orElseThrow(() -> new NotFoundException("Service not found"));
        return toResponse(item);
    }

    /** Lists active service categories in display order. */
    @Transactional(readOnly = true)
    public List<PublicServiceCategoryResponse> listActiveServiceCategories() {
        return serviceCategoryRepository.findByStatusOrderByDisplayOrderAsc(
                        ServiceCategory.STATUS_ACTIVE).stream()
                .map(PublicCatalogService::toCategoryResponse)
                .toList();
    }

    /** Lists published expertise areas in display order. */
    @Transactional(readOnly = true)
    public List<PublicExpertiseAreaResponse> listPublishedExpertiseAreas() {
        return expertiseAreaRepository.findByStatusOrderByDisplayOrderAsc(
                        ExpertiseArea.STATUS_PUBLISHED).stream()
                .map(PublicCatalogService::toExpertiseResponse)
                .toList();
    }

    static PublicServiceItemResponse toResponse(ServiceItem item) {
        return new PublicServiceItemResponse(
                item.getId(),
                item.getSlug(),
                item.getTitle(),
                item.getSummary(),
                item.getDescription(),
                item.getDisplayOrder(),
                item.getCategory() != null
                        && ServiceCategory.STATUS_ACTIVE.equals(item.getCategory().getStatus())
                        ? toCategoryResponse(item.getCategory()) : null,
                item.getUpdatedAt());
    }

    static PublicServiceCategoryResponse toCategoryResponse(ServiceCategory category) {
        return new PublicServiceCategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getDisplayOrder(),
                category.getUpdatedAt());
    }

    static PublicExpertiseAreaResponse toExpertiseResponse(ExpertiseArea area) {
        return new PublicExpertiseAreaResponse(
                area.getId(),
                area.getSlug(),
                area.getName(),
                area.getDescription(),
                area.getDisplayOrder(),
                area.getUpdatedAt());
    }
}
