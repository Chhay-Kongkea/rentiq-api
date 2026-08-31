package co.istad.rentiq_api.features.financialReport.dto.projection;

import java.math.BigDecimal;
import java.sql.Date;

/**
 * One (period, currency) bucket of a Vendor's COMPLETED booking value trend — marketplace
 * rental GMV, never Vendor wallet earnings. See
 * BookingRepository.aggregateCompletedBookingValueByOwnerAndDay/Month.
 */
public interface VendorBookingValueTrendProjection {

    Date getPeriod();

    String getCurrency();

    BigDecimal getTotalBookingValue();

    Long getBookingCount();
}
