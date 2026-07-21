package co.istad.rentiq_api.features.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required") String currentPassword,
        @NotBlank(message = "New password is required") @Size(min = 8, max = 255) String newPassword,
        @NotBlank(message = "Confirm password is required") String confirmPassword
) {}