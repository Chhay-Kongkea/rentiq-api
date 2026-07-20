package co.istad.rentiq_api.features.search.dto.respone;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SearchLogResponse(
        UUID id,
        String userId,
        String keyword,
        Short categoryId,
        Double latitude,
        Double longitude,
        OffsetDateTime createdAt
) {
}