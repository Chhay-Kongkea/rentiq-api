package co.istad.rentiq_api.features.financialReport.dto.projection;

import java.math.BigDecimal;

/**
 * One currency's totals for a Vendor's COMPLETED bookings in a date range — marketplace rental
 * GMV (Booking.subtotal), never Vendor wallet earnings. See
 * BookingRepository.sumCompletedBookingValueByOwnerAndCurrency.
 */
public interface VendorBookingValueCurrencyProjection {

    String getCurrency();

    BigDecimal getTotalBookingValue();

    Long getBookingCount();
}
