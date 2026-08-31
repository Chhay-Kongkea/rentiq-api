package co.istad.rentiq_api.features.financialReport.dto.response;

import java.util.List;

public record PlatformRevenueCurrencyTrend(
        String currency,
        List<PlatformRevenueTrendPoint> points
) {}
