package co.istad.rentiq_api.features.financialReport.dto.response;

import java.math.BigDecimal;

/** {@code period} is "yyyy-MM-dd" for DAY grouping, "yyyy-MM" for MONTH grouping. */
public record PlatformRevenueTrendPoint(
        String period,
        BigDecimal totalRevenue,
        BigDecimal promotionRevenue,
        BigDecimal advertisementRevenue,
        long transactionCount
) {}
