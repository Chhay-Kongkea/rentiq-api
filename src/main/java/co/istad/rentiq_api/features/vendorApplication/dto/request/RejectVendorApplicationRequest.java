package co.istad.rentiq_api.features.vendorApplication.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectVendorApplicationRequest(
        @NotBlank(message = "Rejection reason is required")
        @Size(max = 1000, message = "Rejection reason cannot exceed 1000 characters")
        String reason
) {
    public RejectVendorApplicationRequest {
        if (reason != null) {
            reason = reason.trim();
        }
    }
}
