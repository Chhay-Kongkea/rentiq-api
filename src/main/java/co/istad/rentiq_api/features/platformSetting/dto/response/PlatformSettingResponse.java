package co.istad.rentiq_api.features.platformSetting.dto.response;

import co.istad.rentiq_api.features.platformSetting.enums.PlatformSettingKey;
import co.istad.rentiq_api.features.platformSetting.enums.SettingCategory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Never exposes persistence internals — no entity leak. {@code overridden} tells the caller
 * whether {@code value} came from a database row or is simply {@code defaultValue} echoed back.
 */
public record PlatformSettingResponse(
        PlatformSettingKey key,
        SettingCategory category,
        String currency,
        BigDecimal defaultValue,
        BigDecimal value,
        boolean overridden,
        String updatedBy,
        OffsetDateTime updatedAt
) {}
