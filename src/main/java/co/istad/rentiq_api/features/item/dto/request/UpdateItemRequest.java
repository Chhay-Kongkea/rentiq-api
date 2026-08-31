package co.istad.rentiq_api.features.item.dto.request;

import co.istad.rentiq_api.features.item.enums.ItemCondition;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public record UpdateItemRequest(

        java.util.UUID categoryId,

        @Size(min = 3, max = 200, message = "Title must contain between 3 and 200 characters")
        String title,

        @Size(max = 5000, message = "Description cannot exceed 5000 characters")
        String description,

        ItemCondition condition,

        Map<String, Object> specifications,

        @Size(min = 1, max = 255, message = "Location cannot exceed 255 characters")
        String locationText,

        @DecimalMin(value = "-90.0", message = "Latitude must be at least -90")
        @DecimalMax(value = "90.0", message = "Latitude must not exceed 90")
        BigDecimal latitude,

        @DecimalMin(value = "-180.0", message = "Longitude must be at least -180")
        @DecimalMax(value = "180.0", message = "Longitude must not exceed 180")
        BigDecimal longitude,

        @DecimalMin(value = "0.01", message = "Price per day must be greater than zero")
        @Digits(integer = 13, fraction = 2, message = "Price per day must contain at most 2 decimal places")
        BigDecimal pricePerDay,

        @DecimalMin(value = "0.00", message = "Deposit amount cannot be negative")
        @Digits(integer = 13, fraction = 2, message = "Deposit amount must contain at most 2 decimal places")
        BigDecimal depositAmount

) {

    public UpdateItemRequest {
        if (title != null) {
            title = title.trim();
        }

        if (description != null) {
            description = description.trim();
        }

        if (locationText != null) {
            locationText = locationText.trim();
        }

        if (specifications != null) {
            specifications = Map.copyOf(specifications);
        }
    }
}
