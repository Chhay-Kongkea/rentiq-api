package co.istad.rentiq_api.features.userProfile.dto.response;


import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record UserProfileResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        String avatarUrl,
        String locale,
        String accountStatus,
        OffsetDateTime memberSince
) {}