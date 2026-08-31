package co.istad.rentiq_api.features.vendorPerformance.dto.response;

import co.istad.rentiq_api.features.userProfile.enums.AccountStatus;

import java.math.BigDecimal;


/**
 * {@code completedBookingValue} is rental GMV arranged through Rentiq for this vendor's
 * COMPLETED bookings (Booking.subtotal) — NOT wallet earnings and NOT money Rentiq collected.
 * Rental payment is P2P: the renter pays the vendor directly, outside Rentiq, so nothing here
 * is sourced from the Wallet ledger (backend audit FIN-004).
 */
public record VendorPerformanceResponse(
        String ownerId,
        AccountStatus accountStatus,
        long totalBookings,
        long completedBookings,
        BigDecimal acceptanceRate,
        BigDecimal cancellationRate,
        BigDecimal averageRating,
        long reviewCount,
        BigDecimal medianResponseTimeMinutes,
        BigDecimal completedBookingValue
) {}
