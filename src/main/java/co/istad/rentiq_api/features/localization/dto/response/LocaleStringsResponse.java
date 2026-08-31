package co.istad.rentiq_api.features.localization.dto.response;

import java.util.Map;

/** {@code strings} is an immutable copy — never the service's internal cached map. */
public record LocaleStringsResponse(
        String code,
        Map<String, String> strings
) {}
