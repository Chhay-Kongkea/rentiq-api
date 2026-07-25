package co.istad.rentiq_api.features.item.dto.request;

import co.istad.rentiq_api.features.item.enums.ItemAvailabilityState;
import jakarta.validation.constraints.NotNull;

public record
UpdateItemAvailabilityRequest(

        @NotNull(message = "Availability state is required")
        ItemAvailabilityState availability

) {
}