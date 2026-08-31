package co.istad.rentiq_api.features.financialReport.dto.response;

import co.istad.rentiq_api.features.financialReport.dto.GroupBy;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Booking financial activity / marketplace GMV — sourced from Booking.subtotal, the rental
 * value arranged through Rentiq. Renters pay vendors directly, outside Rentiq, so this is NOT
 * Rentiq's own earned revenue. For actual Platform Revenue (Promotion + Advertisement wallet
 * charges), use {@code GET /api/v1/admin/financial-reports/platform-revenue}.
 */
public record RevenueReportResponse(
        LocalDate from,
        LocalDate to,
        GroupBy groupBy,
        BigDecimal totalBookingValue,
        long totalBookings,
        Page<RevenuePeriodRow> rows
) {}
