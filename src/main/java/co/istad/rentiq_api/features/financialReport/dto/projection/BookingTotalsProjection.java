package co.istad.rentiq_api.features.financialReport.dto.projection;

import java.math.BigDecimal;

public interface BookingTotalsProjection {

    BigDecimal getTotalRevenue();

    BigDecimal getTotalCommission();

    Long getBookingCount();
}
