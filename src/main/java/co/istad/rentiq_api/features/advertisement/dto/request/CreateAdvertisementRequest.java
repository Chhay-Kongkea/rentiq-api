package co.istad.rentiq_api.features.advertisement.dto.request;

import co.istad.rentiq_api.features.advertisement.enums.AdvertisementPackage;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Deliberately excludes price, currency, durationDays, endAt, status, and vendorId — those are
 * always backend-derived (see AdvertisementPackage / AdvertisementServiceImpl.create), never
 * accepted from the client.
 */
public record CreateAdvertisementRequest(

        @NotNull
        UUID itemId,

        @NotNull
        AdvertisementPackage packageType,

        @NotBlank
        @Size(max = 200)
        String title,

        @Size(max = 5000)
        String description,

        @Size(max = 500)
        String imageUrl,

        @NotNull
        @Future
        OffsetDateTime startAt

) {}
