package co.istad.rentiq_api.features.booking.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        String bookingRef,
        String customerId,
        String ownerId,
        UUID itemId,
        UUID offerId,
        LocalDate rentalStart,
        LocalDate rentalEnd,
        Short rentalDays,
        BigDecimal bookedPricePerDay,
        BigDecimal subtotal,
        BigDecimal securityDeposit,
        BigDecimal commissionRate,
        BigDecimal commissionAmount,
        BigDecimal totalAmount,
        String currency,
        String status,
        String paymentStatus,
        Instant ownerConfirmedAt,
        Instant securityDepositReturnedAt,
        Instant createdAt,
        Instant updatedAt
) {}