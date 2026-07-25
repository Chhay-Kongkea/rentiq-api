package co.istad.rentiq_api.features.bookingDispute.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResolveDisputeRequest(
        @NotBlank @Pattern(regexp = "RESOLVED|REJECTED|CLOSED", message = "status must be RESOLVED, REJECTED or CLOSED")
        String status,
        String notes
) {}