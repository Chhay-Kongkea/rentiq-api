package co.istad.rentiq_api.features.offers.dto.response;

import co.istad.rentiq_api.features.offers.enums.OfferStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OfferStatusResponse(

        UUID id,

        OfferStatus status,

        OffsetDateTime updatedAt

) {}