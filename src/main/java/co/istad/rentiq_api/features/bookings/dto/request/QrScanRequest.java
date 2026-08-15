package co.istad.rentiq_api.features.bookings.dto.request;

import jakarta.validation.constraints.NotBlank;

public record QrScanRequest(

        @NotBlank
        String qrToken

) {}
