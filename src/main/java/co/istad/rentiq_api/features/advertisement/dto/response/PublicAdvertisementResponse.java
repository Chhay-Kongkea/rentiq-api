package co.istad.rentiq_api.features.advertisement.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;
public record PublicAdvertisementResponse(
        UUID id,
        UUID itemId,
        String title,
        String description,
        String imageUrl,
        OffsetDateTime startAt,
        OffsetDateTime endAt
) {}
