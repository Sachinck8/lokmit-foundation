package com.lokmit.foundation.projects.service;

import com.lokmit.foundation.common.exception.ConflictException;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.projects.dto.ProjectImageCreateRequest;
import com.lokmit.foundation.projects.dto.ProjectImageResponse;
import com.lokmit.foundation.projects.dto.ProjectImageUpdateRequest;
import com.lokmit.foundation.projects.entity.Project;
import com.lokmit.foundation.projects.entity.ProjectImage;
import com.lokmit.foundation.projects.repository.ProjectImageRepository;
import com.lokmit.foundation.projects.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Business rules for project image METADATA management (A6).
 *
 * <p>Every image row is owned by a project ({@code project_id} is NOT NULL,
 * FK ON DELETE CASCADE — the schema's designed ownership cleanup). The
 * owning project must exist before any image row is created: an unknown
 * project id is a 404, never an orphaning insert. {@code imageUrl} is a
 * caller-supplied reference; no binary upload, storage or processing exists
 * in A6. The URL is unique within a project's gallery to keep ordering and
 * deduplication predictable — a pure metadata-level rule, not a schema
 * constraint.</p>
 */
@Service
public class ProjectImageService {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectImageService.class);

    private final ProjectImageRepository projectImageRepository;
    private final ProjectRepository projectRepository;

    public ProjectImageService(ProjectImageRepository projectImageRepository,
                               ProjectRepository projectRepository) {
        this.projectImageRepository = projectImageRepository;
        this.projectRepository = projectRepository;
    }

    /** Paginated gallery for one project, lowest display order first, then id. */
    @Transactional(readOnly = true)
    public Page<ProjectImageResponse> listImages(long projectId, Pageable pageable) {
        requireProject(projectId);
        return projectImageRepository.findByProjectId(projectId, pageable)
                .map(ProjectImageService::toResponse);
    }

    /** Fetch one image-metadata row by id, regardless of project. */
    @Transactional(readOnly = true)
    public ProjectImageResponse getImage(long imageId) {
        return projectImageRepository.findById(imageId)
                .map(ProjectImageService::toResponse)
                .orElseThrow(() -> new NotFoundException("Project image not found"));
    }

    /**
     * Adds image metadata to an existing project. Unknown project → 404
     * before any write. Duplicate URL within the same project's gallery →
     * 409 (service-level rule; kept out of the schema deliberately).
     */
    @Transactional
    public ProjectImageResponse createImage(long projectId, ProjectImageCreateRequest request) {
        Project project = requireProject(projectId);

        if (projectImageRepository.existsByProjectIdAndImageUrl(projectId, request.getImageUrl())) {
            throw new ConflictException(
                    "An image with this URL already exists in project " + project.getSlug());
        }

        ProjectImage image = new ProjectImage();
        image.setProject(project);
        image.setImageUrl(request.getImageUrl().trim());
        image.setAltText(request.getAltText());
        image.setCaption(request.getCaption());
        image.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        image.setCreatedAt(OffsetDateTime.now());

        try {
            return toResponse(projectImageRepository.save(image));
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ConflictException(
                    "An image with this URL already exists in project " + project.getSlug());
        }
    }

    /** Fetch one image-metadata row within its owning project; mismatch → 404. */
    @Transactional(readOnly = true)
    public ProjectImageResponse getImageForProject(long projectId, long imageId) {
        return toResponse(requireOwnedImage(projectId, imageId));
    }

    /**
     * Partial edit of image metadata within an owning project. A
     * project/image mismatch is a 404 — the image must belong to the path
     * project. The owning project itself is immutable — images move between
     * projects only by delete + recreate, keeping ownership unambiguous.
     */
    @Transactional
    public ProjectImageResponse updateImageForProject(long projectId, long imageId,
                                                      ProjectImageUpdateRequest request) {
        ProjectImage image = requireOwnedImage(projectId, imageId);

        if (request.isImageUrlProvided()) {
            String url = request.getImageUrl().trim();
            if (projectImageRepository.existsByProjectIdAndImageUrlAndIdNot(projectId, url, imageId)) {
                throw new ConflictException(
                        "An image with this URL already exists in project " + projectId);
            }
            image.setImageUrl(url);
        }
        if (request.isAltTextProvided()) {
            image.setAltText(request.getAltText());
        }
        if (request.isCaptionProvided()) {
            image.setCaption(request.getCaption());
        }
        if (request.getDisplayOrder() != null) {
            image.setDisplayOrder(request.getDisplayOrder());
        }
        return toResponse(projectImageRepository.save(image));
    }

    /**
     * Deletes a single image-metadata row from its owning project. A
     * project/image mismatch is a 404. The owning project is never touched —
     * only the metadata row is removed (the reverse cascade, project delete
     * → images gone, is the FK's business).
     */
    @Transactional
    public void deleteImageForProject(long projectId, long imageId) {
        ProjectImage image = requireOwnedImage(projectId, imageId);
        projectImageRepository.delete(image);
        LOG.info("Project image id {} deleted from project id {}", imageId, projectId);
    }

    /** Loads an image and enforces that it belongs to the path project. */
    private ProjectImage requireOwnedImage(long projectId, long imageId) {
        ProjectImage image = projectImageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("Project image not found"));
        if (image.getProject().getId() != projectId) {
            throw new NotFoundException(
                    "Project image " + imageId + " not found in project " + projectId);
        }
        return image;
    }

    private Project requireProject(long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project " + projectId + " not found"));
    }

    static ProjectImageResponse toResponse(ProjectImage image) {
        return ProjectImageResponse.builder()
                .id(image.getId())
                .project(ProjectImageResponse.ProjectRef.builder()
                        .id(image.getProject().getId())
                        .slug(image.getProject().getSlug())
                        .title(image.getProject().getTitle())
                        .build())
                .imageUrl(image.getImageUrl())
                .altText(image.getAltText())
                .caption(image.getCaption())
                .displayOrder(image.getDisplayOrder())
                .createdAt(image.getCreatedAt())
                .build();
    }
}
