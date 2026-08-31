package co.istad.rentiq_api.features.advertisement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectAdvertisementRequest(

        @NotBlank
        @Size(max = 1000)
        String reason

) {}
