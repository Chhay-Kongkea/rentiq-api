package co.istad.rentiq_api.features.adminUserManagement.dto.request;

import jakarta.validation.constraints.Size;

public record AdminUserModerationRequest(
        @Size(max = 1000, message = "reason must not exceed 1000 characters")
        String reason
) {
}
