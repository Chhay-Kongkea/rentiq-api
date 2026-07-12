package co.istad.rentiq_api.features.item.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateItemAvailabilityRequest(

        @NotNull(message = "Availability is required")
        Boolean available

) {
}