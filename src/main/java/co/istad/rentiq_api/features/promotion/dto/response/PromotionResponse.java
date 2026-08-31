package co.istad.rentiq_api.features.promotion.dto.response;

import co.istad.rentiq_api.features.promotion.enums.PromotionPackage;
import co.istad.rentiq_api.features.promotion.enums.PromotionStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * status is the EFFECTIVE status (an ACTIVE row whose endAt has passed is reported as
 * EXPIRED here even though the stored column hasn't been rewritten — see PromotionMapper).
 */
public record PromotionResponse(
        UUID id,
        String vendorId,
        UUID itemId,
        PromotionPackage packageType,
        int durationDays,
        BigDecimal price,
        String currency,
        PromotionStatus status,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        long impressionCount,
        long clickCount,
        OffsetDateTime cancelledAt,
        String suspendedBy,
        OffsetDateTime suspendedAt,
        String suspensionReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
