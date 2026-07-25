package co.istad.rentiq_api.features.bookingDispute.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDisputeRequest(
        @NotBlank String disputeType,
        @NotBlank @Size(max = 2000) String description
) {}