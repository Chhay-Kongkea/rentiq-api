package co.istad.rentiq_api.features.kyc.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitKycRequest(
        @NotBlank(message = "National ID number is required")
        @Size(max = 255)
        String nationalIdNumber,

        @NotBlank(message = "National ID type is required")
        @Size(max = 50)
        String nationalIdType,

        @Size(min = 3, max = 3, message = "Country must be a 3-letter ISO code")
        String nationalIdCountry
) {}
