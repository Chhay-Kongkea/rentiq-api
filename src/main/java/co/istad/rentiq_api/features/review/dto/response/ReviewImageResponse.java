package co.istad.rentiq_api.features.review.dto.response;

import java.util.UUID;

public record ReviewImageResponse(
        UUID id,
        String imageUrl,
        String thumbnailUrl,
        Short sortOrder
) {}