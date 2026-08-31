package co.istad.rentiq_api.features.platformSetting.service;

import co.istad.rentiq_api.features.platformSetting.dto.request.UpdatePlatformSettingRequest;
import co.istad.rentiq_api.features.platformSetting.dto.response.PlatformSettingResponse;
import co.istad.rentiq_api.features.platformSetting.enums.PlatformSettingKey;

import java.math.BigDecimal;
import java.util.List;

public interface PlatformSettingService {

    List<PlatformSettingResponse> getAllSettings();

    PlatformSettingResponse getSetting(PlatformSettingKey key);

    /**
     * The single runtime read used by PlatformPricingService: the Admin override if one exists,
     * otherwise the key's static default. Never touches the HTTP-facing response DTO.
     */
    BigDecimal getEffectiveValue(PlatformSettingKey key);

    /**
     * Admin identity always comes from the caller (resolved via AuthUtils at the controller),
     * never accepted in the request body.
     */
    PlatformSettingResponse update(PlatformSettingKey key, UpdatePlatformSettingRequest request, String adminId);
}
