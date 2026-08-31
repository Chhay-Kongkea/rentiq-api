package co.istad.rentiq_api.features.itemrequest.dto.request;

import jakarta.validation.constraints.*;

public record NearbyItemRequestFilter(

        @NotNull
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        Double latitude,

        @NotNull
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        Double longitude,

        @DecimalMin("0.1")
        @DecimalMax("200.0")
        Double radiusKm,

        java.util.UUID categoryId,

        @Min(0)
        Integer pageNumber,

        @Min(1)
        @Max(100)
        Integer pageSize

) {
    public NearbyItemRequestFilter {
        radiusKm = radiusKm == null ? 10.0 : radiusKm;
        pageNumber = pageNumber == null ? 0 : pageNumber;
        pageSize = pageSize == null ? 12 : pageSize;
    }
}
