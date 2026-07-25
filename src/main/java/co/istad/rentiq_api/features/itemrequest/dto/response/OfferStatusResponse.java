package co.istad.rentiq_api.features.itemrequest.dto.response;

import co.istad.rentiq_api.features.itemrequest.enums.OfferStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OfferStatusResponse(
        UUID offerId,
        UUID requestId,
        OfferStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
