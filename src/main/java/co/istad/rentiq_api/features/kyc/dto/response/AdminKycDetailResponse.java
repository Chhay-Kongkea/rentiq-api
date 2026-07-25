package co.istad.rentiq_api.features.kyc.dto.response;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record AdminKycDetailResponse(
        UUID id,
        String userId,
        String nationalIdNumber,
        String nationalIdType,
        String nationalIdCountry,
        String frontImageUrl,
        String backImageUrl,
        String phoneNumber,
        boolean emailVerified,
        String verificationStatus,
        String rejectionReason,
        String reviewedBy,
        OffsetDateTime reviewedAt,
        OffsetDateTime verifiedAt,
        OffsetDateTime createdAt
) {}