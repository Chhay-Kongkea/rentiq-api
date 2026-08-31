package co.istad.rentiq_api.features.adminDashboard.dto.response;

import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueCurrencySummary;

import java.math.BigDecimal;
import java.util.List;

/**
 * Two distinct financial concepts, never mixed (backend audit FIN-003):
 * <ul>
 *   <li>{@code totalBookingValue}/{@code todayBookingValue} — marketplace rental GMV
 *       (Booking.subtotal/totalAmount). Renters pay vendors directly, outside Rentiq, so this
 *       is NOT Rentiq's own revenue.</li>
 *   <li>{@code calculatedCommission} — stored per Booking but never actually collected via any
 *       wallet transaction. Calculated only, not Platform Revenue.</li>
 *   <li>{@code platformRevenue} — the actual ledger-backed money Rentiq has earned (Promotion +
 *       Advertisement wallet charges, trailing 12 months), reusing the same
 *       {@link PlatformRevenueCurrencySummary} shape and {@code FinancialReportService} logic
 *       as {@code GET /api/v1/admin/financial-reports/platform-revenue} — one accounting rule,
 *       never a second formula. USD and KHR are always kept separate, never combined.</li>
 * </ul>
 */
public record FinancialDashboardSummaryResponse(
        BigDecimal totalBookingValue,
        BigDecimal todayBookingValue,
        BigDecimal calculatedCommission,
        List<PlatformRevenueCurrencySummary> platformRevenue
) {}
