package co.istad.rentiq_api.features.booking.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ScanResultResponse(
        UUID bookingId,
        String bookingRef,
        String status,
        Instant scannedAt
) {}