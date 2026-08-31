package co.istad.rentiq_api.features.financialReport.dto.projection;

import java.math.BigDecimal;

/** totalBookingValue is marketplace rental GMV (booking subtotal) — never Rentiq's own revenue. */
public interface BookingTotalsProjection {

    BigDecimal getTotalBookingValue();

    BigDecimal getTotalCommission();

    Long getBookingCount();
}
