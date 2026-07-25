package co.istad.rentiq_api.features.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(


        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 255)
        String username,


        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 255)
        String password,

        @NotBlank(message = "Confirm password is required")
        @Size(min = 8, max = 255)
        String confirmPassword,

        @NotBlank(message = "Email is required")
        @Email
        @Size(max = 255)
        String email,


        @NotBlank(message = "First name is required")
        @Size(max = 255)
        String firstName,


        @NotBlank(message = "Last name is required")
        @Size(max = 255)
        String lastName

) {
}
