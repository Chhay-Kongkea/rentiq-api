package co.istad.rentiq_api.features.itemrequest.dto.request;

import co.istad.rentiq_api.features.itemrequest.enums.ItemRequestStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record ItemRequestFilter(

        String keyword,

        java.util.UUID categoryId,

        @DecimalMin("0.0")
        BigDecimal budgetMin,

        @DecimalMin("0.0")
        BigDecimal budgetMax,

        ItemRequestStatus status,

        @Min(0)
        Integer pageNumber,

        @Min(1)
        @Max(100)
        Integer pageSize,

        String sortBy,

        String sortDirection

) {
    public ItemRequestFilter {
        keyword = normalize(keyword);
        pageNumber = pageNumber == null ? 0 : pageNumber;
        pageSize = pageSize == null ? 12 : pageSize;
        sortBy = normalize(sortBy);
        sortDirection = normalize(sortDirection);

        if (sortBy == null) {
            sortBy = "createdAt";
        }

        if (sortDirection == null) {
            sortDirection = "desc";
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}
