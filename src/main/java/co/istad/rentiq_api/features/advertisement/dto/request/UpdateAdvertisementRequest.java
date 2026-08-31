package co.istad.rentiq_api.features.advertisement.dto.request;

import co.istad.rentiq_api.features.advertisement.enums.AdvertisementPackage;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * itemId is intentionally not editable — advertising a different item is a new advertisement,
 * not an edit of this one. Excludes price, currency, durationDays, endAt, and status for the
 * same reason as CreateAdvertisementRequest: those are always backend-derived.
 */
public record UpdateAdvertisementRequest(

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
