package co.istad.rentiq_api.features.adminDashboard.dto.response;

import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueCurrencySummary;

import java.math.BigDecimal;
import java.util.List;
public record FinancialDashboardSummaryResponse(
        BigDecimal totalBookingValue,
        BigDecimal todayBookingValue,
        BigDecimal calculatedCommission,
        List<PlatformRevenueCurrencySummary> platformRevenue
) {}
