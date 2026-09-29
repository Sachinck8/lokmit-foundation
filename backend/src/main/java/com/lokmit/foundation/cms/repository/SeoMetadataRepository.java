package com.lokmit.foundation.cms.repository;

import com.lokmit.foundation.cms.entity.SeoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SeoMetadataRepository
        extends JpaRepository<SeoMetadata, Long>, JpaSpecificationExecutor<SeoMetadata> {

    Optional<SeoMetadata> findByEntityTypeAndEntityId(String entityType, Long entityId);

    boolean existsByEntityTypeAndEntityId(String entityType, Long entityId);
}
