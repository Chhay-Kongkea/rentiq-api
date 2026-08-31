package co.istad.rentiq_api.features.platformSetting.controller;

import co.istad.rentiq_api.features.platformSetting.dto.request.UpdatePlatformSettingRequest;
import co.istad.rentiq_api.features.platformSetting.dto.response.PlatformSettingResponse;
import co.istad.rentiq_api.features.platformSetting.enums.PlatformSettingKey;
import co.istad.rentiq_api.features.platformSetting.service.PlatformSettingService;
import co.istad.rentiq_api.security.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin Settings v1 — manages ONLY the twelve predefined Advertisement/Promotion pricing
 * settings (see PlatformSettingKey). There is no endpoint to create an arbitrary key.
 */
@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettingController {

    private final PlatformSettingService platformSettingService;

    @GetMapping
    public List<PlatformSettingResponse> getAllSettings() {
        return platformSettingService.getAllSettings();
    }

    @GetMapping("/{key}")
    public PlatformSettingResponse getSetting(@PathVariable PlatformSettingKey key) {
        return platformSettingService.getSetting(key);
    }

    @PatchMapping("/{key}")
    public PlatformSettingResponse updateSetting(
            @PathVariable PlatformSettingKey key,
            @Valid @RequestBody UpdatePlatformSettingRequest request
    ) {
        return platformSettingService.update(key, request, AuthUtils.extractUserId());
    }
}
