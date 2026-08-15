package co.istad.rentiq_api.features.adminUserManagement.dto.response;

import co.istad.rentiq_api.features.userProfile.enums.AccountStatus;

import java.time.OffsetDateTime;

public record AdminUserStatusResponse(
        String userId,
        AccountStatus previousStatus,
        AccountStatus accountStatus,
        String reason,
        OffsetDateTime updatedAt
) {
}
