package co.istad.rentiq_api.features.booking.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ScanQrRequest(
        @NotBlank String qrToken
) {}