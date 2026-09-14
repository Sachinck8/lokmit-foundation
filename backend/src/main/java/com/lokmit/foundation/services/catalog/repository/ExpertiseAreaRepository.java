package com.lokmit.foundation.services.catalog.repository;

import com.lokmit.foundation.services.catalog.entity.ExpertiseArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpertiseAreaRepository
        extends JpaRepository<ExpertiseArea, Long>, JpaSpecificationExecutor<ExpertiseArea> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, long id);
}
