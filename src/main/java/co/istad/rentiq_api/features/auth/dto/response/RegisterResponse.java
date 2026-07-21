package co.istad.rentiq_api.features.auth.dto.response;


import lombok.Builder;

@Builder
public record RegisterResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName
) {
}
