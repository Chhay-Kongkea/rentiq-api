package co.istad.rentiq_api.features.item.dto.respone;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AvailabilityBlockResponse(
        UUID id,
        UUID itemId,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        String source,
        OffsetDateTime createdAt
) {
}
