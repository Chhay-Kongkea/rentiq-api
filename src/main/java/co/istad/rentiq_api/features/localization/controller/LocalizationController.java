package co.istad.rentiq_api.features.localization.controller;

import co.istad.rentiq_api.features.localization.dto.response.LocaleStringsResponse;
import co.istad.rentiq_api.features.localization.dto.response.SupportedLocalesResponse;
import co.istad.rentiq_api.features.localization.service.LocalizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * Public, read-only UI string localization. No mutation endpoints exist — translations are
 * static application resources (see LocalizationServiceImpl), not admin-editable content.
 */
@RestController
@RequestMapping("/api/v1/locales")
@RequiredArgsConstructor
public class LocalizationController {

    private static final CacheControl STATIC_RESOURCE_CACHE = CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic();

    private final LocalizationService localizationService;

    @GetMapping
    public ResponseEntity<SupportedLocalesResponse> listLocales() {
        return ResponseEntity.ok()
                .cacheControl(STATIC_RESOURCE_CACHE)
                .body(new SupportedLocalesResponse(localizationService.getSupportedLocales()));
    }

    @GetMapping("/{code}/strings")
    public ResponseEntity<LocaleStringsResponse> getStrings(@PathVariable String code) {
        return ResponseEntity.ok()
                .cacheControl(STATIC_RESOURCE_CACHE)
                .body(localizationService.getStrings(code));
    }
}
