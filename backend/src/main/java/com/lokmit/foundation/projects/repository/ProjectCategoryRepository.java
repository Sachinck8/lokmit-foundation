package com.lokmit.foundation.projects.repository;

import com.lokmit.foundation.projects.entity.ProjectCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectCategoryRepository
        extends JpaRepository<ProjectCategory, Long>, JpaSpecificationExecutor<ProjectCategory> {

    /** Public listing of ACTIVE categories in display order (A26). */
    List<ProjectCategory> findByStatusOrderByDisplayOrderAsc(String status);

    /** A26 public category filter: resolve an ACTIVE category by slug (empty if not ACTIVE). */
    List<ProjectCategory> findByStatusAndSlug(String status, String slug);

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    boolean existsByNameAndIdNot(String name, long id);

    boolean existsBySlugAndIdNot(String slug, long id);
}
