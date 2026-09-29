package com.lokmit.foundation.services.catalog.repository;

import com.lokmit.foundation.services.catalog.entity.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceItemRepository
        extends JpaRepository<ServiceItem, Long>, JpaSpecificationExecutor<ServiceItem> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, long id);

    /** Public detail lookup by slug (A26); status filtering is the caller's job. */
    Optional<ServiceItem> findBySlug(String slug);
}
