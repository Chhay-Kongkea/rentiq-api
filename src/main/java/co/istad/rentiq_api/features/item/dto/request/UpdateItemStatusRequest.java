package co.istad.rentiq_api.features.item.dto.request;

import co.istad.rentiq_api.features.item.enums.ItemStatus;
import jakarta.validation.constraints.NotNull;


public record UpdateItemStatusRequest(

        @NotNull(message = "Item status is required")
        ItemStatus status

) {
}