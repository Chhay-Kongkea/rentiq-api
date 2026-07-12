package co.istad.rentiq_api.features.item.dto.request;

import co.istad.rentiq_api.features.item.enums.ItemCondition;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ItemFilter(

        @Size(max = 200, message = "Keyword cannot exceed 200 characters")
        String keyword,

        @DecimalMin(value = "0.00", message = "Minimum price cannot be negative")
        BigDecimal minPrice,

        @DecimalMin(value = "0.00", message = "Maximum price cannot be negative")
        BigDecimal maxPrice,

        Short categoryId,
        ItemCondition condition,
        Boolean available,
        Boolean featured,

        @Size(max = 255, message = "Location cannot exceed 255 characters")
        String location

) {

    @AssertTrue(message = "Minimum price cannot be greater than maximum price")
    public boolean isPriceRangeValid() {
        return minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0;
    }
}