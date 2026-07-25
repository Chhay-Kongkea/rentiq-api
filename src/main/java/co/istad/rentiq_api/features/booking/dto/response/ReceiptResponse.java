package co.istad.rentiq_api.features.booking.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReceiptResponse(
        UUID bookingId,
        String bookingRef,
        LocalDate rentalStart,
        LocalDate rentalEnd,
        Short rentalDays,
        BigDecimal bookedPricePerDay,
        BigDecimal subtotal,
        BigDecimal securityDeposit,
        BigDecimal totalAmount,
        String currency,
        String paymentStatus,
        Instant issuedAt
) {}