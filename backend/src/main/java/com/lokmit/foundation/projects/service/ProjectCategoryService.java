package com.lokmit.foundation.projects.service;

import com.lokmit.foundation.common.exception.BadRequestException;
import com.lokmit.foundation.common.exception.ConflictException;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.projects.dto.ProjectCategoryCreateRequest;
import com.lokmit.foundation.projects.dto.ProjectCategoryResponse;
import com.lokmit.foundation.projects.dto.ProjectCategoryUpdateRequest;
import com.lokmit.foundation.projects.entity.ProjectCategory;
import com.lokmit.foundation.projects.repository.ProjectCategoryRepository;
import com.lokmit.foundation.projects.repository.ProjectRepository;
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
 * Business rules for project-category management (A6).
 *
 * <p>Same simple catalog design as A5 service categories: two unique
 * business keys (name, slug), an ACTIVE/INACTIVE lifecycle and a display
 * order. Slugs are immutable once created. Deleting a category is safe by
 * schema design: projects referencing it are detached by the existing FK
 * (ON DELETE SET NULL) — nothing cascade-deletes, verified before deleting
 * so the detached count is logged for administrators.</p>
 */
@Service
public class ProjectCategoryService {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectCategoryService.class);

    public static final Set<String> VALID_STATUSES =
            Set.of(ProjectCategory.STATUS_ACTIVE, ProjectCategory.STATUS_INACTIVE);

    private final ProjectCategoryRepository projectCategoryRepository;
    private final ProjectRepository projectRepository;

    public ProjectCategoryService(ProjectCategoryRepository projectCategoryRepository,
                                  ProjectRepository projectRepository) {
        this.projectCategoryRepository = projectCategoryRepository;
        this.projectRepository = projectRepository;
    }

    /**
     * Paginated list, lowest display order first (the catalog's natural
     * ordering), then name. Optional exact status filter validated against
     * the chk_project_categories_status domain.
     */
    @Transactional(readOnly = true)
    public Page<ProjectCategoryResponse> listCategories(String status, Pageable pageable) {
        Specification<ProjectCategory> spec = Specification.where(null);
        if (status != null && !status.isBlank()) {
            String normalized = status.trim().toUpperCase(Locale.ROOT);
            if (!VALID_STATUSES.contains(normalized)) {
                throw new BadRequestException("Status must be one of: ACTIVE, INACTIVE");
            }
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), normalized));
        }
        return projectCategoryRepository.findAll(spec, pageable)
                .map(ProjectCategoryService::toResponse);
    }

    /** Fetch by numeric id. */
    @Transactional(readOnly = true)
    public ProjectCategoryResponse getCategory(long id) {
        return projectCategoryRepository.findById(id)
                .map(ProjectCategoryService::toResponse)
                .orElseThrow(() -> new NotFoundException("Project category not found"));
    }

    /**
     * Creates a category. Always starts ACTIVE (the schema default). Duplicate
     * name or slug is pre-checked for a clean 409 and backstopped by the
     * unique constraints against concurrent creation races.
     */
    @Transactional
    public ProjectCategoryResponse createCategory(ProjectCategoryCreateRequest request) {
        String name = request.getName().trim();
        String slug = request.getSlug().trim();

        if (projectCategoryRepository.existsByName(name)) {
            throw new ConflictException("Project category name '" + name + "' already exists");
        }
        if (projectCategoryRepository.existsBySlug(slug)) {
            throw new ConflictException("Project category slug '" + slug + "' already exists");
        }

        ProjectCategory category = new ProjectCategory();
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(request.getDescription());
        category.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        category.setStatus(ProjectCategory.STATUS_ACTIVE);
        category.setCreatedAt(OffsetDateTime.now());
        category.setUpdatedAt(OffsetDateTime.now());

        try {
            return toResponse(projectCategoryRepository.save(category));
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ConflictException("Project category name or slug already exists");
        }
    }

    /**
     * Partial edit. Slug never moves; omitted fields stay unchanged; status
     * is validated against the schema domain. Unique collisions are
     * pre-checked (excluding the row itself) and backstopped by the
     * constraints.
     */
    @Transactional
    public ProjectCategoryResponse updateCategory(long id, ProjectCategoryUpdateRequest request) {
        ProjectCategory category = projectCategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project category not found"));

        if (request.getName() != null) {
            String name = request.getName().trim();
            if (projectCategoryRepository.existsByNameAndIdNot(name, id)) {
                throw new ConflictException("Project category name '" + name + "' already exists");
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
        return toResponse(projectCategoryRepository.save(category));
    }

    /**
     * Deletes a category. Safe by schema design: projects keep existing, the
     * FK detaches them (ON DELETE SET NULL) — no cascade deletes any
     * business data. The detached-project count is resolved (single COUNT
     * query) purely for the audit log.
     */
    @Transactional
    public void deleteCategory(long id) {
        ProjectCategory category = projectCategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project category not found"));
        long detachedProjects = projectRepository.countByCategoryId(id);
        projectCategoryRepository.delete(category);
        LOG.info("Project category id {} ({}) deleted; {} referencing project(s) become uncategorized",
                id, category.getSlug(), detachedProjects);
    }

    static ProjectCategoryResponse toResponse(ProjectCategory category) {
        return ProjectCategoryResponse.builder()
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
