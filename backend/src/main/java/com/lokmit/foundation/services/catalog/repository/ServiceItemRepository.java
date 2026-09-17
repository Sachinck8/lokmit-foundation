package com.lokmit.foundation.services.catalog.repository;

import com.lokmit.foundation.services.catalog.entity.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceItemRepository
        extends JpaRepository<ServiceItem, Long>, JpaSpecificationExecutor<ServiceItem> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, long id);
}
