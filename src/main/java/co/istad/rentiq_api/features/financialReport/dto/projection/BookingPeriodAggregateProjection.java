package co.istad.rentiq_api.features.financialReport.dto.projection;

import java.math.BigDecimal;
import java.sql.Date;

/** totalBookingValue is marketplace rental GMV (booking subtotal) — never Rentiq's own revenue. */
public interface BookingPeriodAggregateProjection {

    Date getPeriod();

    BigDecimal getTotalBookingValue();

    BigDecimal getTotalCommission();

    Long getBookingCount();
}
