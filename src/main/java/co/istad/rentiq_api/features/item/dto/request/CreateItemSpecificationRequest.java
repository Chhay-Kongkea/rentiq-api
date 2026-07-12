package co.istad.rentiq_api.features.item.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateItemSpecificationRequest(

        @NotBlank(message = "Specification key is required")
        @Size(max = 100, message = "Specification key cannot exceed 100 characters")
        String key,

        @NotBlank(message = "Specification value is required")
        @Size(max = 500, message = "Specification value cannot exceed 500 characters")
        String value,

        @Size(max = 50, message = "Specification unit cannot exceed 50 characters")
        String unit,

        @Min(value = 0, message = "Sort order cannot be negative")
        Integer sortOrder

) {

    public CreateItemSpecificationRequest {
        if (key != null) {
            key = key.trim();
        }

        if (value != null) {
            value = value.trim();
        }

        if (unit != null) {
            unit = unit.trim();
        }

        if (sortOrder == null) {
            sortOrder = 0;
        }
    }
}