package co.istad.rentiq_api.features.kyc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectKycRequest(
        @NotBlank(message = "Rejection reason is required")
        @Size(max = 1000)
        String reason
) {}