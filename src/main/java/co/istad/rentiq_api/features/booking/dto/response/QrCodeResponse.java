package co.istad.rentiq_api.features.booking.dto.response;

import java.time.Instant;

public record QrCodeResponse(
        String qrToken,
        Instant expiresAt
) {}