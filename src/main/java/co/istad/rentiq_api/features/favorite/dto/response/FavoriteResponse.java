package co.istad.rentiq_api.features.favorite.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FavoriteResponse(
        UUID itemId,
        String title,
        String thumbnailUrl,
        BigDecimal pricePerDay,
        BigDecimal averageRating,
        Integer totalReviews,
        String locationText,
        Instant favoritedAt
) {}