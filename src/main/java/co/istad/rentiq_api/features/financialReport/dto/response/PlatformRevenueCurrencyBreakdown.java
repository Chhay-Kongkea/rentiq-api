package co.istad.rentiq_api.features.financialReport.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record PlatformRevenueCurrencyBreakdown(
        String currency,
        BigDecimal totalRevenue,
        List<PlatformRevenueSourceBreakdown> sources
) {}
