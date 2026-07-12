package co.istad.rentiq_api.features.item.dto.request;
import co.istad.rentiq_api.features.item.enums.ItemCondition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CreateItemRequest(

        @NotNull(message = "Category is required")
        Short categoryId,

        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 200, message = "Title must contain between 3 and 200 characters")
        String title,

        @Size(max = 5000, message = "Description cannot exceed 5000 characters")
        String description,

        @NotNull(message = "Item condition is required")
        ItemCondition condition,

        Map<String, Object> specifications,

        @NotBlank(message = "Location is required")
        @Size(max = 255, message = "Location cannot exceed 255 characters")
        String locationText,

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be at least -90")
        @DecimalMax(value = "90.0", message = "Latitude must not exceed 90")
        BigDecimal latitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be at least -180")
        @DecimalMax(value = "180.0", message = "Longitude must not exceed 180")
        BigDecimal longitude,

        @NotNull(message = "Price per day is required")
        @DecimalMin(value = "0.01", message = "Price per day must be greater than zero")
        @Digits(integer = 13, fraction = 2, message = "Price per day must contain at most 2 decimal places")
        BigDecimal pricePerDay,

        @DecimalMin(value = "0.00", message = "Deposit amount cannot be negative")
        @Digits(integer = 13, fraction = 2, message = "Deposit amount must contain at most 2 decimal places")
        BigDecimal depositAmount

) {

    public CreateItemRequest {
        if (title != null) {
            title = title.trim();
        }

        if (description != null) {
            description = description.trim();
        }

        if (locationText != null) {
            locationText = locationText.trim();
        }

        specifications = specifications == null
                ? Map.of() : Map.copyOf(specifications);

        depositAmount = depositAmount == null
                ? BigDecimal.ZERO : depositAmount;
    }
}