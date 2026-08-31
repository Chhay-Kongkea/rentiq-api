package co.istad.rentiq_api.features.promotion.dto.response;

import co.istad.rentiq_api.features.promotion.enums.PromotionPackage;
import co.istad.rentiq_api.features.promotion.enums.PromotionStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PromotionStatsResponse(
        UUID promotionId,
        UUID itemId,
        PromotionPackage packageType,
        PromotionStatus status,
        BigDecimal price,
        String currency,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        long impressions,
        long clicks,
        BigDecimal ctr
) {}
