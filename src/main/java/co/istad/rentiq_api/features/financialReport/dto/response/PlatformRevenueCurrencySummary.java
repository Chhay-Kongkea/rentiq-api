package co.istad.rentiq_api.features.financialReport.dto.response;

import java.math.BigDecimal;

public record PlatformRevenueCurrencySummary(
        String currency,
        BigDecimal totalRevenue,
        BigDecimal promotionRevenue,
        BigDecimal advertisementRevenue,
        long totalTransactions,
        long promotionTransactions,
        long advertisementTransactions
) {}
