package co.istad.rentiq_api.features.userProfile.dto.response;


import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record AddressResponse(
        UUID id,
        String addressLine,
        String city,
        String country,
        Double latitude,
        Double longitude,
        boolean isDefault,
        OffsetDateTime createdAt
) {}
