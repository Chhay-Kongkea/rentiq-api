package co.istad.rentiq_api.features.advertisement.dto.response;

import co.istad.rentiq_api.features.advertisement.enums.AdvertisementPackage;
import co.istad.rentiq_api.features.advertisement.enums.AdvertisementStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Vendor/Admin management view — includes moderation metadata and purchase details (package,
 * price, currency). Never returned to the public discovery endpoints (see
 * PublicAdvertisementResponse for that contract) — public users need campaign content, not
 * vendor billing data. price/currency are null until a successful admin approval freezes them.
 */
public record AdvertisementResponse(
        UUID id,
        String vendorId,
        UUID itemId,
        String title,
        String description,
        String imageUrl,
        AdvertisementPackage packageType,
        Integer durationDays,
        BigDecimal quotedPrice,
        String quotedCurrency,
        OffsetDateTime quotedAt,
        BigDecimal price,
        String currency,
        AdvertisementStatus status,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String rejectionReason,
        String reviewedBy,
        OffsetDateTime reviewedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
