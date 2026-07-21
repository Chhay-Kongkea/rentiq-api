package co.istad.rentiq_api.features.bookingDispute.dto.response;

import java.time.Instant;
import java.util.UUID;

public record DisputeResponse(
        UUID id,
        UUID bookingId,
        String openedBy,
        String disputeType,
        String description,
        String status,
        String resolvedBy,
        Instant resolvedAt,
        Instant createdAt
) {}