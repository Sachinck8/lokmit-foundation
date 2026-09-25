package com.lokmit.foundation.services.catalog.repository;

import com.lokmit.foundation.services.catalog.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceCategoryRepository
        extends JpaRepository<ServiceCategory, Long>, JpaSpecificationExecutor<ServiceCategory> {

    /** Public listing of ACTIVE categories in display order (A26). */
    List<ServiceCategory> findByStatusOrderByDisplayOrderAsc(String status);

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    boolean existsByNameAndIdNot(String name, long id);

    boolean existsBySlugAndIdNot(String slug, long id);
}
