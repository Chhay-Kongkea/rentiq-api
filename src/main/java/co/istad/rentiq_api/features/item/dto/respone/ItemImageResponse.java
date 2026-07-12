package co.istad.rentiq_api.features.item.dto.respone;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ItemImageResponse(
        UUID id,
        String imageUrl,
        String thumbnailUrl,
        Integer sortOrder,
        boolean primary,
        OffsetDateTime createdAt
) {
}
