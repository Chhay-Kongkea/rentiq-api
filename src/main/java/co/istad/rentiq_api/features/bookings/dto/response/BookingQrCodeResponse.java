package co.istad.rentiq_api.features.bookings.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingQrCodeResponse(

        UUID bookingId,

        String qrToken,

        String qrImageBase64,

        OffsetDateTime expiresAt

) {}
