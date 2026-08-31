package co.istad.rentiq_api.features.itemrequest.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CreateItemRequestRequest(

        @NotNull(message = "Category is required")
        java.util.UUID categoryId,

        @NotBlank(message = "Title is required")
        @Size(max = 200)
        String title,

        @Size(max = 3000)
        String description,

        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal budgetMin,

        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal budgetMax,

        @NotNull(message = "Needed-from date is required")
        @FutureOrPresent
        LocalDate neededFrom,

        @NotNull(message = "Needed-to date is required")
        @FutureOrPresent
        LocalDate neededTo,

        @NotNull(message = "Latitude is required")
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        Double latitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        Double longitude,

        @Min(1)
        @Max(200)
        Short radiusKm,

        @Future
        OffsetDateTime expiresAt

) {
    public CreateItemRequestRequest {
        title = normalize(title);
        description = normalize(description);

        if (radiusKm == null) {
            radiusKm = 10;
        }
    }

    @AssertTrue(message = "Maximum budget must be greater than or equal to minimum budget")
    public boolean isBudgetRangeValid() {
        return budgetMin == null
                || budgetMax == null
                || budgetMax.compareTo(budgetMin) >= 0;
    }

    @AssertTrue(message = "Needed-to date must be on or after needed-from date")
    public boolean isDateRangeValid() {
        return neededFrom == null
                || neededTo == null
                || !neededTo.isBefore(neededFrom);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
