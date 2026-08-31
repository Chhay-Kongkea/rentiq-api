package co.istad.rentiq_api.features.financialReport.service;

import co.istad.rentiq_api.features.financialReport.dto.GroupBy;
import co.istad.rentiq_api.features.financialReport.dto.response.CommissionTimeSeriesResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueBreakdownResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueSummaryResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueTrendResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.RevenueReportResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.TransactionReportResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.VendorBookingValueReportResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public interface FinancialReportService {

    RevenueReportResponse getRevenueReport(LocalDate from, LocalDate to, GroupBy groupBy, Pageable pageable);

    CommissionTimeSeriesResponse getCommissionReport(LocalDate from, LocalDate to, GroupBy groupBy, Pageable pageable);

    TransactionReportResponse getTransactionReport(LocalDate from, LocalDate to, GroupBy groupBy, Pageable pageable);

    /**
     * A single Vendor's own COMPLETED booking value (marketplace rental GMV), per currency —
     * never Vendor wallet earnings, never Rentiq Platform Revenue. {@code ownerId} must be the
     * authenticated caller's own id (server-resolved), never accepted from request input.
     */
    VendorBookingValueReportResponse getVendorBookingValueReport(String ownerId, LocalDate from, LocalDate to, GroupBy groupBy);

    byte[] exportRevenuePdf(LocalDate from, LocalDate to, GroupBy groupBy);

    byte[] exportRevenueXlsx(LocalDate from, LocalDate to, GroupBy groupBy);

    /**
     * Platform Revenue = successful PROMOTION + ADVERTISEMENT OUT WalletTransaction charges
     * only — never booking value, never TOP_UP, never ADMIN_ADJUSTMENT. Reported per currency,
     * never combined. {@code to} is exclusive: [from, to).
     */
    PlatformRevenueSummaryResponse getPlatformRevenueSummary(OffsetDateTime from, OffsetDateTime to);

    PlatformRevenueTrendResponse getPlatformRevenueTrend(OffsetDateTime from, OffsetDateTime to, GroupBy groupBy);

    PlatformRevenueBreakdownResponse getPlatformRevenueBreakdown(OffsetDateTime from, OffsetDateTime to);
}
