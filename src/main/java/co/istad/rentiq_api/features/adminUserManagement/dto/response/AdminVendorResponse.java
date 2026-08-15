package co.istad.rentiq_api.features.adminUserManagement.dto.response;

import co.istad.rentiq_api.features.userProfile.enums.AccountStatus;
import co.istad.rentiq_api.features.wallet.enums.WalletStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
public record AdminVendorResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        String avatarUrl,
        AccountStatus accountStatus,
        boolean enabled,
        boolean emailVerified,
        String kycStatus,
        BigDecimal walletBalance,
        String walletCurrency,
        WalletStatus walletStatus,
        long totalItems,
        long totalBookings,
        BigDecimal averageRating,
        long totalReviews,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
