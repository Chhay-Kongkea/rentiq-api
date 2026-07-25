package co.istad.rentiq_api.features.kyc.dto.response;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record KycResponse(
        UUID id,
        String nationalIdNumber,
        String nationalIdType,
        String nationalIdCountry,
        String frontImageUrl,
        String backImageUrl,
        String phoneNumber,

        boolean emailVerified,
        String verificationStatus,
        String rejectionReason,
        OffsetDateTime verifiedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}