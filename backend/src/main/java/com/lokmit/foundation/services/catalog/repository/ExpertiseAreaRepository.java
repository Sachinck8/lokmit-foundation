package com.lokmit.foundation.services.catalog.repository;

import com.lokmit.foundation.services.catalog.entity.ExpertiseArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpertiseAreaRepository
        extends JpaRepository<ExpertiseArea, Long>, JpaSpecificationExecutor<ExpertiseArea> {

    /** Public listing of PUBLISHED areas in display order (A26). */
    List<ExpertiseArea> findByStatusOrderByDisplayOrderAsc(String status);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, long id);
}
