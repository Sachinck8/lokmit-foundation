package com.lokmit.foundation.projects.repository;

import com.lokmit.foundation.projects.entity.ProjectImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectImageRepository extends JpaRepository<ProjectImage, Long> {

    /** Paginated image list for one project, display order applied by the caller. */
    Page<ProjectImage> findByProjectId(Long projectId, Pageable pageable);

    /** Uniqueness check for an image URL within one project's gallery. */
    boolean existsByProjectIdAndImageUrl(Long projectId, String imageUrl);

    boolean existsByProjectIdAndImageUrlAndIdNot(Long projectId, String imageUrl, long id);
}
