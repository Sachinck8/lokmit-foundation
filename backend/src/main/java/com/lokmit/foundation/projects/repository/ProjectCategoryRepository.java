package com.lokmit.foundation.projects.repository;

import com.lokmit.foundation.projects.entity.ProjectCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectCategoryRepository
        extends JpaRepository<ProjectCategory, Long>, JpaSpecificationExecutor<ProjectCategory> {

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    boolean existsByNameAndIdNot(String name, long id);

    boolean existsBySlugAndIdNot(String slug, long id);
}
