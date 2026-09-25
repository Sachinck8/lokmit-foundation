package com.lokmit.foundation.projects.service;

import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.projects.dto.PublicProjectCategoryResponse;
import com.lokmit.foundation.projects.dto.PublicProjectResponse;
import com.lokmit.foundation.projects.entity.Project;
import com.lokmit.foundation.projects.entity.ProjectCategory;
import com.lokmit.foundation.projects.repository.ProjectCategoryRepository;
import com.lokmit.foundation.projects.repository.ProjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Public (anonymous) projects browsing (A26) over the existing V- tables.
 *
 * <p>Visibility rules, mirroring the A8 public-jobs convention: only
 * {@code status = PUBLISHED} projects are public; a project whose category is
 * INACTIVE keeps its category data hidden (nulled) rather than leaking an
 * inactive category through a published project. Delivery state
 * ({@code projectStatus}) is public project information and stays.</p>
 *
 * <p>Unknown slug AND known-but-not-published slug both produce the identical
 * {@link NotFoundException} — the endpoint never reveals whether an
 * unpublished project exists.</p>
 */
@Service
public class PublicProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectCategoryRepository projectCategoryRepository;

    public PublicProjectService(ProjectRepository projectRepository,
                                ProjectCategoryRepository projectCategoryRepository) {
        this.projectRepository = projectRepository;
        this.projectCategoryRepository = projectCategoryRepository;
    }

    /**
     * Lists published projects, newest first. Optional category filter by
     * ACTIVE category slug — an unknown or non-ACTIVE category slug yields an
     * empty page rather than leaking the category's existence.
     */
    @Transactional(readOnly = true)
    public Page<PublicProjectResponse> listPublishedProjects(String categorySlug, Pageable pageable) {
        Specification<Project> spec = (root, query, cb) ->
                cb.equal(root.get("status"), Project.STATUS_PUBLISHED);
        if (StringUtils.hasText(categorySlug)) {
            List<Long> ids = projectCategoryRepository
                    .findByStatusAndSlug(ProjectCategory.STATUS_ACTIVE, categorySlug.trim())
                    .stream().map(ProjectCategory::getId).toList();
            if (ids.isEmpty()) {
                return Page.empty(pageable);
            }
            spec = spec.and((root, query, cb) -> root.get("category").get("id").in(ids));
        }
        return projectRepository.findAll(spec, pageable).map(PublicProjectService::toResponse);
    }

    /**
     * Fetches one published project by slug. Unknown slug and
     * known-but-not-published slug are indistinguishable (404).
     */
    @Transactional(readOnly = true)
    public PublicProjectResponse getPublishedProject(String slug) {
        Project project = projectRepository.findBySlug(slug)
                .filter(p -> Project.STATUS_PUBLISHED.equals(p.getStatus()))
                .orElseThrow(() -> new NotFoundException("Project not found"));
        return toResponse(project);
    }

    /** Lists active project categories in display order. */
    @Transactional(readOnly = true)
    public List<PublicProjectCategoryResponse> listActiveProjectCategories() {
        return projectCategoryRepository.findByStatusOrderByDisplayOrderAsc(
                        ProjectCategory.STATUS_ACTIVE).stream()
                .map(PublicProjectService::toCategoryResponse)
                .toList();
    }

    static PublicProjectResponse toResponse(Project project) {
        return new PublicProjectResponse(
                project.getId(),
                project.getSlug(),
                project.getTitle(),
                project.getSummary(),
                project.getDescription(),
                project.getCategory() != null
                        && ProjectCategory.STATUS_ACTIVE.equals(project.getCategory().getStatus())
                        ? toCategoryResponse(project.getCategory()) : null,
                project.getProjectStatus(),
                project.getLocation(),
                project.getStartDate(),
                project.getEndDate(),
                project.getObjectives(),
                project.getImpactSummary(),
                project.getPublishedAt(),
                project.getUpdatedAt());
    }

    static PublicProjectCategoryResponse toCategoryResponse(ProjectCategory category) {
        return new PublicProjectCategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getDisplayOrder(),
                category.getUpdatedAt());
    }
}
