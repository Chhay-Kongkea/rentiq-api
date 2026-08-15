package co.istad.rentiq_api.features.vendorPerformance.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VendorModerationRequest(

        @NotBlank
        String reason

) {}
