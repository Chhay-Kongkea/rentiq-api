package co.istad.rentiq_api.features.userProfile.dto.request;


import jakarta.validation.constraints.Pattern;

public record UpdateProfileRequest(
        @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "Locale must be like 'en' or 'en-US'")
        String locale
) {}