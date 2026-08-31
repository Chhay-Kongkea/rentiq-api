package co.istad.rentiq_api.features.adminDashboard.projection;

import java.math.BigDecimal;

/**
 * totalBookingValue is marketplace rental GMV (booking subtotal/total), never Rentiq's own
 * revenue. calculatedCommission is stored on each Booking but never actually collected via any
 * wallet transaction — see FinancialDashboardSummaryResponse for the ledger-backed Platform
 * Revenue figure, which is the only field that represents money Rentiq has actually earned.
 */
public interface DashboardFinancialProjection {
    BigDecimal getTotalBookingValue();
    BigDecimal getCalculatedCommission();
}
