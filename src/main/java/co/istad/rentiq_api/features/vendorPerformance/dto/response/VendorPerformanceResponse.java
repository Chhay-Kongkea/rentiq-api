package co.istad.rentiq_api.features.vendorPerformance.dto.response;

import co.istad.rentiq_api.features.userProfile.enums.AccountStatus;

import java.math.BigDecimal;


public record VendorPerformanceResponse(
        String ownerId,
        AccountStatus accountStatus,
        long totalBookings,
        long completedBookings,
        BigDecimal acceptanceRate,
        BigDecimal cancellationRate,
        BigDecimal averageRating,
        long reviewCount,
        BigDecimal medianResponseTimeMinutes,
        BigDecimal totalEarnings
) {}
