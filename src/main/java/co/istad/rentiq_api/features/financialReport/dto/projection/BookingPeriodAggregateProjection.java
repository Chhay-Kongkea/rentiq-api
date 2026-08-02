package co.istad.rentiq_api.features.financialReport.dto.projection;

import java.math.BigDecimal;
import java.sql.Date;

public interface BookingPeriodAggregateProjection {

    Date getPeriod();

    BigDecimal getTotalRevenue();

    BigDecimal getTotalCommission();

    Long getBookingCount();
}
