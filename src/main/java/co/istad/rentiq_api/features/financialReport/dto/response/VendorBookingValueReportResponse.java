package co.istad.rentiq_api.features.financialReport.dto.response;

import co.istad.rentiq_api.features.financialReport.dto.GroupBy;

import java.time.LocalDate;
import java.util.List;

/**
 * A Vendor's own COMPLETED booking value (marketplace rental GMV) for a date range — never
 * Vendor wallet earnings, never Rentiq Platform Revenue. Rental payment is P2P: the renter pays
 * the Vendor directly, outside Rentiq, so this figure is what was arranged through Rentiq's
 * marketplace, not proof of money actually received. USD and KHR are always reported
 * separately in {@code currencies}/{@code trend}, never combined, no conversion.
 */
public record VendorBookingValueReportResponse(
        LocalDate from,
        LocalDate to,
        GroupBy groupBy,
        List<VendorBookingValueCurrencySummary> currencies,
        List<VendorBookingValueCurrencyTrend> trend
) {}
