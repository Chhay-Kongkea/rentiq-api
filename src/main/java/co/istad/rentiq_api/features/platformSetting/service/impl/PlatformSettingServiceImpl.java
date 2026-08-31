package co.istad.rentiq_api.features.platformSetting.service.impl;

import co.istad.rentiq_api.common.exception.InvalidOperationException;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
import co.istad.rentiq_api.features.platformSetting.dto.request.UpdatePlatformSettingRequest;
import co.istad.rentiq_api.features.platformSetting.dto.response.PlatformSettingResponse;
import co.istad.rentiq_api.features.platformSetting.entity.PlatformSetting;
import co.istad.rentiq_api.features.platformSetting.enums.PlatformSettingKey;
import co.istad.rentiq_api.features.platformSetting.repository.PlatformSettingRepository;
import co.istad.rentiq_api.features.platformSetting.service.PlatformSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformSettingServiceImpl implements PlatformSettingService {

    private final PlatformSettingRepository platformSettingRepository;
    private final AdminAuditService adminAuditService;

    @Override
    public List<PlatformSettingResponse> getAllSettings() {
        Map<PlatformSettingKey, PlatformSetting> overrides = platformSettingRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(PlatformSetting::getKey, s -> s));

        return java.util.Arrays.stream(PlatformSettingKey.values())
                .map(key -> toResponse(key, overrides.get(key)))
                .toList();
    }

    @Override
    public PlatformSettingResponse getSetting(PlatformSettingKey key) {
        return toResponse(key, platformSettingRepository.findById(key).orElse(null));
    }

    @Override
    public BigDecimal getEffectiveValue(PlatformSettingKey key) {
        return platformSettingRepository.findById(key)
                .map(PlatformSetting::getValue)
                .orElse(key.getDefaultValue());
    }

    @Override
    @Transactional
    public PlatformSettingResponse update(PlatformSettingKey key, UpdatePlatformSettingRequest request, String adminId) {
        validatePrice(key, request.value());

        BigDecimal previousEffective = getEffectiveValue(key);
        Optional<PlatformSetting> existing = platformSettingRepository.findByKeyForUpdate(key);

        boolean resetToDefault = request.value().compareTo(key.getDefaultValue()) == 0;

        PlatformSetting saved;
        if (resetToDefault) {
            // Prefer removing the override row entirely so overridden=false and the effective
            // value naturally falls back to the default — no separate reset endpoint needed.
            existing.ifPresent(platformSettingRepository::delete);
            saved = null;
        } else {
            PlatformSetting setting = existing.orElseGet(() -> PlatformSetting.builder().key(key).build());
            setting.setValue(request.value());
            setting.setUpdatedBy(adminId);
            saved = platformSettingRepository.save(setting);
        }

        adminAuditService.record(
                AdminAuditAction.PLATFORM_SETTING_UPDATED,
                AdminAuditTargetType.PLATFORM_SETTING,
                key.name(),
                Map.of("key", key.name(), "value", previousEffective),
                Map.of("key", key.name(), "value", request.value()),
                request.reason());

        return toResponse(key, saved);
    }

    private void validatePrice(PlatformSettingKey key, BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("PlatformSetting", "Price must be greater than zero");
        }

        BigDecimal stripped = value.stripTrailingZeros();
        if ("USD".equals(key.getCurrency())) {
            if (stripped.scale() > 2) {
                throw new InvalidOperationException("PlatformSetting", "USD price must have at most 2 decimal places");
            }
        } else if ("KHR".equals(key.getCurrency())) {
            if (stripped.scale() > 0) {
                throw new InvalidOperationException("PlatformSetting", "KHR price must be a whole number");
            }
        }
    }

    private PlatformSettingResponse toResponse(PlatformSettingKey key, PlatformSetting override) {
        boolean overridden = override != null;
        return new PlatformSettingResponse(
                key,
                key.getCategory(),
                key.getCurrency(),
                key.getDefaultValue(),
                overridden ? override.getValue() : key.getDefaultValue(),
                overridden,
                overridden ? override.getUpdatedBy() : null,
                overridden ? override.getUpdatedAt() : null);
    }
}
