package co.istad.rentiq_api.features.item.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record SetItemFeaturedRequest(

        @NotNull(message = "Featured flag is required")
        Boolean featured,

        @Future(message = "Featured until must be a future date")
        OffsetDateTime featuredUntil

) {
}
