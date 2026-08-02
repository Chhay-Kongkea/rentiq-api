package co.istad.rentiq_api.features.vendorPerformance.dto.response;

import co.istad.rentiq_api.features.userProfile.enums.AccountStatus;
import co.istad.rentiq_api.features.vendorPerformance.enums.VendorModerationAction;

import java.time.OffsetDateTime;

public record VendorModerationResponse(
        String targetId,
        AccountStatus accountStatus,
        VendorModerationAction action,
        String reason,
        String adminId,
        OffsetDateTime createdAt
) {}
