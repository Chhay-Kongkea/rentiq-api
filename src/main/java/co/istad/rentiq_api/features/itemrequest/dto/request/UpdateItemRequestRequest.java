package co.istad.rentiq_api.features.itemrequest.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record UpdateItemRequestRequest(

        java.util.UUID categoryId,

        @Size(max = 200)
        String title,

        @Size(max = 3000)
        String description,

        @DecimalMin("0.0")
        BigDecimal budgetMin,

        @DecimalMin("0.0")
        BigDecimal budgetMax,

        @FutureOrPresent
        LocalDate neededFrom,

        @FutureOrPresent
        LocalDate neededTo,

        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        Double latitude,

        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        Double longitude,

        @Min(1)
        @Max(200)
        Short radiusKm,

        @Future
        OffsetDateTime expiresAt

) {
    public UpdateItemRequestRequest {
        title = normalize(title);
        description = normalize(description);
    }

    @AssertTrue(
            message = "Latitude and longitude must be provided together"
    )
    public boolean isLocationValid() {
        return latitude == null && longitude == null
                || latitude != null && longitude != null;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
