package co.istad.rentiq_api.features.localization.dto.response;

public record LocaleResponse(
        String code,
        String name,
        String nativeName
) {}
