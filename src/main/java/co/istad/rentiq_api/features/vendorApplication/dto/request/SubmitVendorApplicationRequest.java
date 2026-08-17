package co.istad.rentiq_api.features.vendorApplication.dto.request;

import jakarta.validation.constraints.Size;

public record SubmitVendorApplicationRequest(
        @Size(max = 2000, message = "Application message cannot exceed 2000 characters")
        String message
) {
    public SubmitVendorApplicationRequest {
        if (message != null) {
            message = message.trim();
            if (message.isBlank()) {
                message = null;
            }
        }
    }
}
