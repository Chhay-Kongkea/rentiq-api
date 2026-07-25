package co.istad.rentiq_api.features.booking.dto.response;

import java.time.Instant;
import java.util.UUID;

public record BookingStatusHistoryResponse(
        UUID id,
        String oldStatus,
        String newStatus,
        String changedBy,
        String reason,
        Instant createdAt
) {}