package co.istad.rentiq_api.features.adminDashboard.projection;

import java.math.BigDecimal;

public interface DashboardFinancialProjection {
    BigDecimal getTotalBookingValue();
    BigDecimal getCalculatedCommission();
}
