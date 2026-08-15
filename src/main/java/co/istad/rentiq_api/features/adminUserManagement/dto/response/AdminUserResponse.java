package co.istad.rentiq_api.features.adminUserManagement.dto.response;

import co.istad.rentiq_api.features.userProfile.enums.AccountStatus;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.Set;

@Builder
public record AdminUserResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        String avatarUrl,
        String locale,
        AccountStatus accountStatus,
        boolean enabled,
        boolean emailVerified,
        Set<String> roles,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
