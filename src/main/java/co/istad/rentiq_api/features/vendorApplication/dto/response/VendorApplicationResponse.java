package co.istad.rentiq_api.features.vendorApplication.dto.response;

import co.istad.rentiq_api.features.vendorApplication.enums.VendorApplicationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VendorApplicationResponse(
        UUID id,
        VendorApplicationStatus status,
        String message,
        String rejectionReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
