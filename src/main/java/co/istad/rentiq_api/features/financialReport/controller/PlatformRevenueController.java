package co.istad.rentiq_api.features.financialReport.controller;

import co.istad.rentiq_api.features.financialReport.dto.GroupBy;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueBreakdownResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueSummaryResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueTrendResponse;
import co.istad.rentiq_api.features.financialReport.service.FinancialReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/**
 * Platform Revenue = successful PROMOTION + ADVERTISEMENT OUT WalletTransaction charges only —
 * never booking value (rental payment is P2P, outside Rentiq), never TOP_UP (wallet funding,
 * not earned revenue), never ADMIN_ADJUSTMENT. See FinancialReportServiceImpl for the ledger
 * query. Distinct from the existing /api/v1/admin/reports/revenue (booking-subtotal-based) —
 * that endpoint is left untouched; see the final report for the semantic conflict this exposed.
 */
@RestController
@RequestMapping("/api/v1/admin/financial-reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PlatformRevenueController {

    private final FinancialReportService financialReportService;

    @GetMapping("/platform-revenue")
    public PlatformRevenueSummaryResponse getPlatformRevenueSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        return financialReportService.getPlatformRevenueSummary(from, to);
    }

    @GetMapping("/platform-revenue/trend")
    public PlatformRevenueTrendResponse getPlatformRevenueTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "DAY") GroupBy groupBy
    ) {
        return financialReportService.getPlatformRevenueTrend(from, to, groupBy);
    }

    @GetMapping("/platform-revenue/breakdown")
    public PlatformRevenueBreakdownResponse getPlatformRevenueBreakdown(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        return financialReportService.getPlatformRevenueBreakdown(from, to);
    }
}
