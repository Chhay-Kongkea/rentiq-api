package co.istad.rentiq_api.features.search.dto.request;

import co.istad.rentiq_api.features.item.enums.ItemCondition;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ItemSearchFilter(

        @Size(max = 255, message = "Keyword cannot exceed 255 characters")
        String keyword,

        @DecimalMin(value = "0.0", message = "Minimum price cannot be negative")
        BigDecimal minPrice,

        @DecimalMin(value = "0.0", message = "Maximum price cannot be negative")
        BigDecimal maxPrice,

        java.util.UUID categoryId,

        ItemCondition condition,

        Boolean available,

        Boolean featured,

        @Size(max = 255, message = "Location cannot exceed 255 characters")
        String location,

        @DecimalMin(value = "0.0", message = "Minimum rating cannot be negative")
        @DecimalMax(value = "5.0", message = "Minimum rating cannot exceed 5")
        BigDecimal minimumRating,

        @Min(value = 0)
        Integer pageNumber,

        @Min(value = 1)
        @Max(value = 100)
        Integer pageSize,

        String sortBy,

        String sortDirection

) {
    public ItemSearchFilter {
        keyword = normalize(keyword);
        location = normalize(location);
        sortBy = normalize(sortBy);
        sortDirection = normalize(sortDirection);

        pageNumber = pageNumber == null
                ? 0
                : pageNumber;

        pageSize = pageSize == null
                ? 12
                : pageSize;

        sortBy = sortBy == null
                ? "createdAt"
                : sortBy;

        sortDirection = sortDirection == null
                ? "desc"
                : sortDirection;
    }

    @AssertTrue(message = "Minimum price cannot exceed maximum price")
    public boolean isPriceRangeValid() {
        return minPrice == null
                || maxPrice == null
                || minPrice.compareTo(maxPrice) <= 0;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
