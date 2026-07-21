package co.istad.rentiq_api.features.search.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record NearbyItemSearchFilter(

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude cannot be less than -90")
        @DecimalMax(value = "90.0", message = "Latitude cannot exceed 90")
        Double latitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude cannot be less than -180")
        @DecimalMax(value = "180.0", message = "Longitude cannot exceed 180")
        Double longitude,

        @DecimalMin(value = "0.1", message = "Radius must be at least 0.1 km")
        @DecimalMax(value = "200.0", message = "Radius cannot exceed 200 km")
        Double radiusKm,

        Short categoryId,

        @Min(0)
        Integer pageNumber,

        @Min(1)
        @Max(100)
        Integer pageSize

) {
    public NearbyItemSearchFilter {
        radiusKm = radiusKm == null
                ? 10.0
                : radiusKm;

        pageNumber = pageNumber == null
                ? 0
                : pageNumber;

        pageSize = pageSize == null
                ? 12
                : pageSize;
    }
}