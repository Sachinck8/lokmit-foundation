package com.lokmit.foundation.projects.repository;

import com.lokmit.foundation.projects.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectRepository
        extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, long id);

    /** Used to report the detached-project count when a category is deleted. */
    long countByCategoryId(Long categoryId);

    /** Public detail lookup by slug (A26); status filtering is the caller's job. */
    Optional<Project> findBySlug(String slug);
}
