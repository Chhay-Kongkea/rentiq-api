package co.istad.rentiq_api.features.bookingDispute.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateDisputeRequest(
        String disputeType,
        @Size(max = 2000) String description
) {}