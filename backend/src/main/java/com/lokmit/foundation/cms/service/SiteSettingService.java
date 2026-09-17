package com.lokmit.foundation.cms.service;

import com.lokmit.foundation.cms.dto.SiteSettingResponse;
import com.lokmit.foundation.cms.dto.SiteSettingUpdateRequest;
import com.lokmit.foundation.cms.entity.SiteSetting;
import com.lokmit.foundation.cms.repository.SiteSettingRepository;
import com.lokmit.foundation.common.exception.BadRequestException;
import com.lokmit.foundation.common.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;

/**
 * Administrative site-settings management (A4).
 *
 * <p>Settings are configuration key/value pairs: the key is the immutable
 * identity, so creation/deletion of keys is out of scope for the management
 * API and only value/description updates are permitted. Keys can be located
 * by exact key or narrowed with a database-side key-prefix filter; values
 * are returned verbatim — they are site configuration, not secrets.</p>
 */
@Service
public class SiteSettingService {

    private static final Logger LOG = LoggerFactory.getLogger(SiteSettingService.class);

    private final SiteSettingRepository siteSettingRepository;

    public SiteSettingService(SiteSettingRepository siteSettingRepository) {
        this.siteSettingRepository = siteSettingRepository;
    }

    /**
     * Paginated settings list, newest first, optional case-insensitive
     * contains-filter on the setting key.
     */
    @Transactional(readOnly = true)
    public Page<SiteSettingResponse> listSettings(String search, Pageable pageable) {
        Specification<SiteSetting> spec = Specification.where(null);
        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("settingKey")), like));
        }
        return siteSettingRepository.findAll(spec, pageable).map(SiteSettingService::toResponse);
    }

    /** Fetch by numeric id. */
    @Transactional(readOnly = true)
    public SiteSettingResponse getSetting(long id) {
        return siteSettingRepository.findById(id)
                .map(SiteSettingService::toResponse)
                .orElseThrow(() -> new NotFoundException("Site setting not found"));
    }

    /** Fetch by exact unique key. */
    @Transactional(readOnly = true)
    public SiteSettingResponse getSettingByKey(String key) {
        return siteSettingRepository.findBySettingKey(key)
                .map(SiteSettingService::toResponse)
                .orElseThrow(() -> new NotFoundException("Site setting not found"));
    }

    /**
     * Partial update of value/description. The key never moves. Concurrent
     * duplicate-key creations are impossible by unique constraint; the
     * integrity-violation backstop converts any race into the standard 409.
     */
    @Transactional
    public SiteSettingResponse updateSetting(long id, SiteSettingUpdateRequest request) {
        SiteSetting setting = siteSettingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Site setting not found"));

        applyUpdate(setting, request);
        try {
            return toResponse(siteSettingRepository.save(setting));
        } catch (DataIntegrityViolationException ex) {
            throw new com.lokmit.foundation.common.exception.ConflictException(
                    "Site setting conflict");
        }
    }

    /** Partial update addressed by the unique setting key. */
    @Transactional
    public SiteSettingResponse updateSettingByKey(String key, SiteSettingUpdateRequest request) {
        SiteSetting setting = siteSettingRepository.findBySettingKey(key)
                .orElseThrow(() -> new NotFoundException("Site setting not found"));

        applyUpdate(setting, request);
        try {
            return toResponse(siteSettingRepository.save(setting));
        } catch (DataIntegrityViolationException ex) {
            throw new com.lokmit.foundation.common.exception.ConflictException(
                    "Site setting conflict");
        }
    }

    private void applyUpdate(SiteSetting setting, SiteSettingUpdateRequest request) {
        if (request.isValueProvided() && request.getSettingValue() == null) {
            throw new BadRequestException("Setting value cannot be null");
        }
        if (request.isValueProvided()) {
            setting.setSettingValue(request.getSettingValue());
        }
        if (request.isDescriptionProvided()) {
            setting.setDescription(request.getDescription());
        }
        setting.setUpdatedAt(OffsetDateTime.now());
        LOG.info("Site setting id {} updated", setting.getId());
    }

    static SiteSettingResponse toResponse(SiteSetting setting) {
        return SiteSettingResponse.builder()
                .id(setting.getId())
                .settingKey(setting.getSettingKey())
                .settingValue(setting.getSettingValue())
                .description(setting.getDescription())
                .createdAt(setting.getCreatedAt())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }
}
