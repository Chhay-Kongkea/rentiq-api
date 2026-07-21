package co.istad.rentiq_api.features.kyc.dto.response;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record AdminKycListItemResponse(
        UUID id,
        String userId,
        String nationalIdNumber,
        String verificationStatus,
        boolean emailVerified,
        OffsetDateTime createdAt
) {}