package com.lokmit.foundation.cms.repository;

import com.lokmit.foundation.cms.entity.WebsiteContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebsiteContentRepository
        extends JpaRepository<WebsiteContent, Long>, JpaSpecificationExecutor<WebsiteContent> {

    Optional<WebsiteContent> findByPageKeyAndSectionKey(String pageKey, String sectionKey);

    boolean existsByPageKeyAndSectionKey(String pageKey, String sectionKey);
}
