package co.istad.rentiq_api.features.vendorApplication.dto.response;

import co.istad.rentiq_api.features.vendorApplication.enums.VendorApplicationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminVendorApplicationResponse(
        UUID id,
        String userId,
        VendorApplicationStatus status,
        String message,
        String rejectionReason,
        String reviewedBy,
        OffsetDateTime reviewedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
