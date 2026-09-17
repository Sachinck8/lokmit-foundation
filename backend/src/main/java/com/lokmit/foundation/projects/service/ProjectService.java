package com.lokmit.foundation.projects.service;

import com.lokmit.foundation.common.exception.BadRequestException;
import com.lokmit.foundation.common.exception.ConflictException;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.projects.dto.ProjectCreateRequest;
import com.lokmit.foundation.projects.dto.ProjectResponse;
import com.lokmit.foundation.projects.dto.ProjectUpdateRequest;
import com.lokmit.foundation.projects.entity.Project;
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
 * Business rules for project management (A6).
 *
 * <p>Two independent status dimensions, exactly per the schema: the
 * editorial lifecycle {@code status} (DRAFT/PUBLISHED/ARCHIVED, shared
 * domain with A4/A5) and the nullable delivery state {@code projectStatus}
 * (PLANNING/ONGOING/COMPLETED). Lifecycle rules mirror A4/A5: new projects
 * start DRAFT; publish is allowed from DRAFT (already-published rejected)
 * and stamps {@code published_at}; archive is allowed from DRAFT or
 * PUBLISHED and is terminal. Referenced categories are validated against
 * the project_categories table — an unknown category id is a 404, never an
 * orphaning insert. chk_projects_dates (end >= start) is enforced in the
 * service so the API answers 400 before the database would reject it.</p>
 */
@Service
public class ProjectService {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectService.class);

    public static final Set<String> VALID_STATUSES =
            Set.of(Project.STATUS_DRAFT, Project.STATUS_PUBLISHED, Project.STATUS_ARCHIVED);

    public static final Set<String> VALID_PROJECT_STATUSES =
            Set.of(Project.PROJECT_STATUS_PLANNING, Project.PROJECT_STATUS_ONGOING,
                    Project.PROJECT_STATUS_COMPLETED);

    private final ProjectRepository projectRepository;
    private final ProjectCategoryRepository projectCategoryRepository;

    public ProjectService(ProjectRepository projectRepository,
                          ProjectCategoryRepository projectCategoryRepository) {
        this.projectRepository = projectRepository;
        this.projectCategoryRepository = projectCategoryRepository;
    }

    /**
     * Paginated list, newest first (portfolio convention). Optional exact
     * filters: categoryId (positive when present), status and projectStatus
     * (validated against their chk domains), plus a case-insensitive
     * title/summary search — all parameterized through Specifications, so
     * the filtering runs database-side over the existing idx_projects_*
     * indexes.
     */
    @Transactional(readOnly = true)
    public Page<ProjectResponse> listProjects(Long categoryId, String status,
                                              String projectStatus, String search,
                                              Pageable pageable) {
        Specification<Project> spec = Specification.where(null);
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
        if (projectStatus != null && !projectStatus.isBlank()) {
            String normalized = projectStatus.trim().toUpperCase(Locale.ROOT);
            if (!VALID_PROJECT_STATUSES.contains(normalized)) {
                throw new BadRequestException("Project status must be one of: PLANNING, ONGOING, COMPLETED");
            }
            spec = spec.and((root, query, cb) -> cb.equal(root.get("projectStatus"), normalized));
        }
        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("summary")), like)));
        }
        return projectRepository.findAll(spec, pageable)
                .map(ProjectService::toResponse);
    }

    /** Fetch by numeric id. */
    @Transactional(readOnly = true)
    public ProjectResponse getProject(long id) {
        return projectRepository.findById(id)
                .map(ProjectService::toResponse)
                .orElseThrow(() -> new NotFoundException("Project not found"));
    }

    /**
     * Creates a project. Always starts DRAFT with a null published_at.
     * Duplicate slug is pre-checked for a clean 409 and backstopped by
     * uq_projects_slug. A provided categoryId must reference an existing
     * category (404 otherwise). Objectives JSON is validated before the
     * write; date pairs are validated against chk_projects_dates.
     */
    @Transactional
    public ProjectResponse createProject(ProjectCreateRequest request) {
        String slug = request.getSlug().trim();

        ProjectJsonValidator.requireWellFormedJson(request.getObjectives());
        validateDatePair(request.getStartDate(), request.getEndDate());

        if (projectRepository.existsBySlug(slug)) {
            throw new ConflictException("Project slug '" + slug + "' already exists");
        }
        ProjectCategory category = resolveCategory(request.getCategoryId());

        Project project = new Project();
        project.setSlug(slug);
        project.setTitle(request.getTitle().trim());
        project.setSummary(request.getSummary());
        project.setDescription(request.getDescription());
        project.setCategory(category);
        project.setStatus(Project.STATUS_DRAFT);
        project.setProjectStatus(normalizeProjectStatus(request.getProjectStatus()));
        project.setLocation(request.getLocation());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setObjectives(request.getObjectives());
        project.setImpactSummary(request.getImpactSummary());
        project.setCreatedAt(OffsetDateTime.now());
        project.setUpdatedAt(OffsetDateTime.now());

        try {
            return toResponse(projectRepository.save(project));
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ConflictException("Project slug '" + slug + "' already exists");
        }
    }

    /**
     * Partial edit. Slug never moves; omitted fields stay unchanged;
     * nullable fields set explicitly to null clear them; categoryId
     * explicitly set to null detaches the project (uncategorized); a new
     * category id is validated (404 when unknown). Status is not editable
     * here — lifecycle transitions use the dedicated endpoints.
     */
    @Transactional
    public ProjectResponse updateProject(long id, ProjectUpdateRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        if (request.isObjectivesProvided() && request.getObjectives() != null) {
            ProjectJsonValidator.requireWellFormedJson(request.getObjectives());
        }

        // Effective date pair after this patch, validated like the DB would.
        validateDatePair(
                request.isStartDateProvided() ? request.getStartDate() : project.getStartDate(),
                request.isEndDateProvided() ? request.getEndDate() : project.getEndDate());

        if (request.getTitle() != null) {
            project.setTitle(request.getTitle().trim());
        }
        if (request.isSummaryProvided()) {
            project.setSummary(request.getSummary());
        }
        if (request.isDescriptionProvided()) {
            project.setDescription(request.getDescription());
        }
        if (request.isCategoryIdProvided()) {
            project.setCategory(resolveCategory(request.getCategoryId()));
        }
        if (request.isProjectStatusProvided()) {
            project.setProjectStatus(normalizeProjectStatus(request.getProjectStatus()));
        }
        if (request.isLocationProvided()) {
            project.setLocation(request.getLocation());
        }
        if (request.isStartDateProvided()) {
            project.setStartDate(request.getStartDate());
        }
        if (request.isEndDateProvided()) {
            project.setEndDate(request.getEndDate());
        }
        if (request.isObjectivesProvided()) {
            project.setObjectives(request.getObjectives());
        }
        if (request.isImpactSummaryProvided()) {
            project.setImpactSummary(request.getImpactSummary());
        }
        project.setUpdatedAt(OffsetDateTime.now());
        return toResponse(projectRepository.save(project));
    }

    /** Publishes a DRAFT project and stamps published_at (idempotence rejected). */
    @Transactional
    public ProjectResponse publishProject(long id) {
        Project project = requireTransitionable(id, Project.STATUS_PUBLISHED);
        project.setStatus(Project.STATUS_PUBLISHED);
        project.setPublishedAt(OffsetDateTime.now());
        project.setUpdatedAt(OffsetDateTime.now());
        ProjectResponse response = toResponse(projectRepository.save(project));
        LOG.info("Project id {} ({}) published", id, project.getSlug());
        return response;
    }

    /** Archives a DRAFT or PUBLISHED project; archiving is terminal. */
    @Transactional
    public ProjectResponse archiveProject(long id) {
        Project project = requireTransitionable(id, Project.STATUS_ARCHIVED);
        project.setStatus(Project.STATUS_ARCHIVED);
        project.setUpdatedAt(OffsetDateTime.now());
        ProjectResponse response = toResponse(projectRepository.save(project));
        LOG.info("Project id {} ({}) transitioned to ARCHIVED", id, project.getSlug());
        return response;
    }

    private Project requireTransitionable(long id, String targetStatus) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        if (Project.STATUS_ARCHIVED.equals(project.getStatus())) {
            throw new ConflictException("Archived projects cannot change status");
        }
        if (Project.STATUS_PUBLISHED.equals(targetStatus)
                && Project.STATUS_PUBLISHED.equals(project.getStatus())) {
            throw new ConflictException("Project is already published");
        }
        return project;
    }

    /**
     * Deletes a project. The existing fk_project_images_project FK
     * (ON DELETE CASCADE) removes the project's image metadata rows — that
     * is the schema's designed ownership cleanup, not business-data loss.
     * No other table references projects, so deletion is self-contained
     * beyond that owned-child cleanup.
     */
    @Transactional
    public void deleteProject(long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        projectRepository.delete(project);
        LOG.info("Project id {} ({}) deleted; owned image metadata rows removed by the FK cascade",
                id, project.getSlug());
    }

    /** Resolves and validates a category reference; null stays uncategorized. */
    private ProjectCategory resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return projectCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Project category " + categoryId + " not found"));
    }

    /**
     * Validates the resulting date pair against chk_projects_dates:
     * end_date >= start_date whenever both are present. When exactly one
     * side is being replaced by a patch, the caller supplies the OTHER
     * side's current value, so a patch can never silently create a pair the
     * database constraint would reject.
     */
    private void validateDatePair(java.time.LocalDate start, java.time.LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new BadRequestException("End date must be on or after the start date");
        }
    }

    private String normalizeProjectStatus(String projectStatus) {
        if (projectStatus == null) {
            return null;
        }
        return projectStatus.trim().toUpperCase(Locale.ROOT);
    }

    static ProjectResponse toResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .slug(project.getSlug())
                .title(project.getTitle())
                .summary(project.getSummary())
                .description(project.getDescription())
                .category(project.getCategory() != null
                        ? ProjectCategoryService.toResponse(project.getCategory()) : null)
                .status(project.getStatus())
                .projectStatus(project.getProjectStatus())
                .location(project.getLocation())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .objectives(project.getObjectives())
                .impactSummary(project.getImpactSummary())
                .publishedAt(project.getPublishedAt())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
