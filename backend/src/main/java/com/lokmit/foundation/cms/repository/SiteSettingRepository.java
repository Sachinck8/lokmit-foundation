package com.lokmit.foundation.cms.repository;

import com.lokmit.foundation.cms.entity.SiteSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SiteSettingRepository
        extends JpaRepository<SiteSetting, Long>, JpaSpecificationExecutor<SiteSetting> {

    Optional<SiteSetting> findBySettingKey(String settingKey);

    boolean existsBySettingKey(String settingKey);
}
