package co.istad.rentiq_api.features.localization.service;

import co.istad.rentiq_api.features.localization.dto.response.LocaleResponse;
import co.istad.rentiq_api.features.localization.dto.response.LocaleStringsResponse;

import java.util.List;

public interface LocalizationService {

    List<LocaleResponse> getSupportedLocales();

    /**
     * Case-insensitive lookup. Throws if {@code code} is not one of the supported locales —
     * never falls back to English, never returns an empty map.
     */
    LocaleStringsResponse getStrings(String code);
}
