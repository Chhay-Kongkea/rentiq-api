package co.istad.rentiq_api.features.financialReport.dto.response;

import java.math.BigDecimal;

/**
 * {@code completedBookingValue} is marketplace rental value (Booking.subtotal) for this
 * Vendor's COMPLETED bookings in one currency — NOT wallet earnings, NOT money Rentiq
 * collected, and NOT commission-adjusted. Rental payment is P2P: the renter pays the Vendor
 * directly, outside Rentiq, so Rentiq has no record of what was actually received.
 */
public record VendorBookingValueCurrencySummary(
        String currency,
        BigDecimal completedBookingValue,
        long completedBookingCount,
        BigDecimal averageBookingValue
) {}
