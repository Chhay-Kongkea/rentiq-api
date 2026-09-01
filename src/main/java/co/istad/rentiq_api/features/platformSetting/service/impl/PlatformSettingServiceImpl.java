package co.istad.rentiq_api.features.platformSetting.service.impl;

import co.istad.rentiq_api.common.exception.InvalidOperationException;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
import co.istad.rentiq_api.features.localization.enums.SupportedLocale;
import co.istad.rentiq_api.features.platformSetting.dto.request.UpdatePlatformSettingRequest;
import co.istad.rentiq_api.features.platformSetting.dto.response.PlatformSettingResponse;
import co.istad.rentiq_api.features.platformSetting.entity.PlatformSetting;
import co.istad.rentiq_api.features.platformSetting.enums.PlatformSettingKey;
import co.istad.rentiq_api.features.platformSetting.enums.SettingValueType;
import co.istad.rentiq_api.features.platformSetting.repository.PlatformSettingRepository;
import co.istad.rentiq_api.features.platformSetting.service.PlatformSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformSettingServiceImpl implements PlatformSettingService {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final PlatformSettingRepository platformSettingRepository;
    private final AdminAuditService adminAuditService;

    @Override
    public List<PlatformSettingResponse> getAllSettings() {
        Map<PlatformSettingKey, PlatformSetting> overrides = platformSettingRepository.findAll().stream()
                .collect(Collectors.toMap(PlatformSetting::getKey, setting -> setting));
        return Arrays.stream(PlatformSettingKey.values())
                .map(key -> toResponse(key, overrides.get(key))).toList();
    }

    @Override
    public PlatformSettingResponse getSetting(PlatformSettingKey key) {
        return toResponse(key, platformSettingRepository.findById(key).orElse(null));
    }

    @Override
    public BigDecimal getEffectiveValue(PlatformSettingKey key) {
        return getDecimal(key);
    }

    @Override
    public BigDecimal getDecimal(PlatformSettingKey key) {
        requireType(key, SettingValueType.DECIMAL);
        return numericValue(key);
    }

    @Override
    public int getInteger(PlatformSettingKey key) {
        requireType(key, SettingValueType.INTEGER);
        return numericValue(key).intValueExact();
    }

    @Override
    public boolean getBoolean(PlatformSettingKey key) {
        requireType(key, SettingValueType.BOOLEAN);
        return numericValue(key).compareTo(BigDecimal.ONE) == 0;
    }

    @Override
    public String getString(PlatformSettingKey key) {
        if (!key.isTextual()) {
            throw new IllegalArgumentException(key + " is not a textual setting");
        }
        return platformSettingRepository.findById(key)
                .map(PlatformSetting::getTextValue).orElse(key.getDefaultTextValue());
    }

    @Override
    @Transactional
    public PlatformSettingResponse update(PlatformSettingKey key, UpdatePlatformSettingRequest request, String adminId) {
        Object normalized = normalizeAndValidate(key, request.value());
        Object previousEffective = effectiveObject(key, platformSettingRepository.findById(key).orElse(null));
        Optional<PlatformSetting> existing = platformSettingRepository.findByKeyForUpdate(key);
        boolean resetToDefault = valuesEqual(normalized, defaultObject(key));

        PlatformSetting saved = null;
        if (resetToDefault) {
            existing.ifPresent(platformSettingRepository::delete);
        } else {
            PlatformSetting setting = existing.orElseGet(() -> PlatformSetting.builder().key(key).build());
            if (key.isTextual()) {
                setting.setValue(null);
                setting.setTextValue((String) normalized);
            } else {
                setting.setValue((BigDecimal) normalized);
                setting.setTextValue(null);
            }
            setting.setUpdatedBy(adminId);
            saved = platformSettingRepository.save(setting);
        }

        adminAuditService.record(
                AdminAuditAction.PLATFORM_SETTING_UPDATED,
                AdminAuditTargetType.PLATFORM_SETTING,
                key.name(),
                Map.of("key", key.name(), "value", previousEffective),
                Map.of("key", key.name(), "value", normalized),
                request.reason());
        return toResponse(key, saved);
    }

    private Object normalizeAndValidate(PlatformSettingKey key, Object raw) {
        if (key.isTextual()) {
            if (!(raw instanceof String text)) {
                throw invalid(key + " must be a string");
            }
            String value = text.trim();
            if (value.isBlank()) throw invalid(key + " cannot be blank");
            if (key == PlatformSettingKey.PLATFORM_DISPLAY_NAME && value.length() > 100)
                throw invalid("Platform display name cannot exceed 100 characters");
            if (key == PlatformSettingKey.SUPPORT_EMAIL
                    && (value.length() > 254 || !EMAIL.matcher(value).matches()))
                throw invalid("Support email must be a valid email address");
            if (key == PlatformSettingKey.DEFAULT_LOCALE) {
                value = SupportedLocale.fromCode(value)
                        .map(SupportedLocale::getCode)
                        .orElseThrow(() -> invalid("Default locale must be en or km"));
            }
            return value;
        }

        BigDecimal value = toDecimal(raw, key);
        if (key.getValueType() == SettingValueType.BOOLEAN) {
            if (value.compareTo(BigDecimal.ZERO) != 0 && value.compareTo(BigDecimal.ONE) != 0)
                throw invalid(key + " must be 0 or 1");
            return value;
        }
        if (key.getValueType() == SettingValueType.INTEGER) {
            try { value.toBigIntegerExact(); }
            catch (ArithmeticException exception) { throw invalid(key + " must be a whole number"); }
            if (value.compareTo(key.getMinimumValue()) < 0 || value.compareTo(key.getMaximumValue()) > 0)
                throw invalid(key + " must be between " + key.getMinimumValue() + " and " + key.getMaximumValue());
            return value;
        }
        validatePrice(key, value);
        return value;
    }

    private BigDecimal toDecimal(Object raw, PlatformSettingKey key) {
        if (raw instanceof Boolean bool && key.getValueType() == SettingValueType.BOOLEAN)
            return bool ? BigDecimal.ONE : BigDecimal.ZERO;
        if (!(raw instanceof Number number)) throw invalid(key + " must be numeric");
        try { return new BigDecimal(number.toString()); }
        catch (NumberFormatException exception) { throw invalid(key + " must be numeric"); }
    }

    private void validatePrice(PlatformSettingKey key, BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) <= 0) throw invalid("Price must be greater than zero");
        int scale = value.stripTrailingZeros().scale();
        if ("USD".equals(key.getCurrency()) && scale > 2) throw invalid("USD price must have at most 2 decimal places");
        if ("KHR".equals(key.getCurrency()) && scale > 0) throw invalid("KHR price must be a whole number");
    }

    private BigDecimal numericValue(PlatformSettingKey key) {
        return platformSettingRepository.findById(key).map(PlatformSetting::getValue).orElse(key.getDefaultValue());
    }

    private Object defaultObject(PlatformSettingKey key) {
        return key.isTextual() ? key.getDefaultTextValue() : key.getDefaultValue();
    }

    private Object effectiveObject(PlatformSettingKey key, PlatformSetting override) {
        if (override == null) return responseValue(key, defaultObject(key));
        return responseValue(key, key.isTextual() ? override.getTextValue() : override.getValue());
    }

    private Object responseValue(PlatformSettingKey key, Object stored) {
        return switch (key.getValueType()) {
            case BOOLEAN -> ((BigDecimal) stored).compareTo(BigDecimal.ONE) == 0;
            case INTEGER -> ((BigDecimal) stored).intValueExact();
            default -> stored;
        };
    }

    private boolean valuesEqual(Object left, Object right) {
        if (left instanceof BigDecimal a && right instanceof BigDecimal b) return a.compareTo(b) == 0;
        return left.equals(right);
    }

    private void requireType(PlatformSettingKey key, SettingValueType type) {
        if (key.getValueType() != type) throw new IllegalArgumentException(key + " is not a " + type + " setting");
    }

    private InvalidOperationException invalid(String message) {
        return new InvalidOperationException("PlatformSetting", message);
    }

    private PlatformSettingResponse toResponse(PlatformSettingKey key, PlatformSetting override) {
        boolean overridden = override != null;
        return new PlatformSettingResponse(
                key, key.getCategory(), key.getValueType(), key.getLabel(), key.getDescription(), key.getCurrency(),
                responseValue(key, defaultObject(key)), effectiveObject(key, override), true, overridden,
                overridden ? override.getUpdatedBy() : null, overridden ? override.getUpdatedAt() : null);
    }
}
