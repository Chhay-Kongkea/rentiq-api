package co.istad.rentiq_api.features.booking.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InvoiceResponse(
        UUID bookingId,
        String bookingRef,
        String ownerId,
        BigDecimal subtotal,
        BigDecimal commissionRate,
        BigDecimal commissionAmount,
        BigDecimal totalAmount,
        String currency,
        Instant issuedAt
) {}