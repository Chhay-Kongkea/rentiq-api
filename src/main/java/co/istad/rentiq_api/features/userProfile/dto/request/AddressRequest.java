package co.istad.rentiq_api.features.userProfile.dto.request;



import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank(message = "Address line is required")
        @Size(max = 255)
        String addressLine,

        @NotBlank(message = "City is required")
        @Size(max = 100)
        String city,

        @Size(min = 3, max = 3, message = "Country must be a 3-letter ISO code")
        String country,

        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
        @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
        @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
        Double longitude,

        Boolean isDefault
) {}