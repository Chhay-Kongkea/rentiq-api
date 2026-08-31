package co.istad.rentiq_api.features.financialReport.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/** totalBookingValue is marketplace rental GMV (booking subtotal) — never Rentiq's own revenue. */
public record RevenuePeriodRow(
        LocalDate period,
        BigDecimal totalBookingValue,
        long bookingCount
) {}
