package co.istad.rentiq_api.features.userProfile.dto.response;


import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record PublicUserProfileResponse(
        String id,
        String username,
        String firstName,
        String lastName,
        String avatarUrl,
        OffsetDateTime memberSince
) {}