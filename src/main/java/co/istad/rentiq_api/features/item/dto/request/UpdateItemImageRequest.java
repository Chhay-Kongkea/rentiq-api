package co.istad.rentiq_api.features.item.dto.request;

import jakarta.validation.constraints.Min;

public record UpdateItemImageRequest(

        @Min(value = 0, message = "Sort order cannot be negative")
        Integer sortOrder,

        Boolean primary

) {
}
