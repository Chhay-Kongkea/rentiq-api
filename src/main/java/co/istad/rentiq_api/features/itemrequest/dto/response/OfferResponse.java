package co.istad.rentiq_api.features.itemrequest.dto.response;

import co.istad.rentiq_api.features.itemrequest.enums.OfferStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OfferResponse(
        UUID id,
        UUID requestId,
        String ownerId,
        UUID itemId,
        String itemTitle,
        BigDecimal offeredPrice,
        String currency,
        String message,
        OfferStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}