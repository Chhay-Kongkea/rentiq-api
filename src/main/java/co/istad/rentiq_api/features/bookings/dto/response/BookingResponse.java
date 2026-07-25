package co.istad.rentiq_api.features.bookings.dto.response;

import co.istad.rentiq_api.features.bookings.enums.BookingStatus;
import co.istad.rentiq_api.features.bookings.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingResponse(

        UUID id,

        String bookingRef,

        UUID itemId,

        UUID offerId,

        String customerId,

        String ownerId,

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

        BookingStatus status,

        PaymentStatus paymentStatus,

        OffsetDateTime ownerConfirmedAt,

        OffsetDateTime securityDepositReturnedAt,

        OffsetDateTime createdAt,

        OffsetDateTime updatedAt

) {}
