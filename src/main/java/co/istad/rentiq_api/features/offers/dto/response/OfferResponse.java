package co.istad.rentiq_api.features.offers.dto.response;

import co.istad.rentiq_api.features.offers.enums.OfferStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OfferResponse(

        UUID id,

        UUID itemId,

        String requesterId,

        String vendorId,

        BigDecimal offeredPrice,

        String currency,

        String message,

        OfferStatus status,

        OffsetDateTime createdAt,

        OffsetDateTime updatedAt

) {
}