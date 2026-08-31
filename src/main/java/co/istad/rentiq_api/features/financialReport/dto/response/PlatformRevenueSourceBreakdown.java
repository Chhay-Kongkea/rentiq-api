package co.istad.rentiq_api.features.financialReport.dto.response;

import java.math.BigDecimal;

/** {@code source} is "PROMOTION" or "ADVERTISEMENT". percentage is 0 when the currency total is 0. */
public record PlatformRevenueSourceBreakdown(
        String source,
        BigDecimal revenue,
        long transactionCount,
        BigDecimal percentage
) {}
