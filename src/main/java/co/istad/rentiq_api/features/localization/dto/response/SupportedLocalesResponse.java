package co.istad.rentiq_api.features.localization.dto.response;

import java.util.List;

public record SupportedLocalesResponse(
        List<LocaleResponse> locales
) {}
