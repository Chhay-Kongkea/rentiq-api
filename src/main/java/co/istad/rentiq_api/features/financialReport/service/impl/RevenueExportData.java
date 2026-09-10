package co.istad.rentiq_api.features.financialReport.service.impl;

import co.istad.rentiq_api.features.financialReport.dto.response.CommissionTimeSeriesResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueSummaryResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.RevenueReportResponse;

import java.time.OffsetDateTime;

/**
 * Export-only bundle of report data already produced by {@code FinancialReportServiceImpl} —
 * the PDF/XLSX generator reads these values as-is and never recalculates them. Booking GMV and
 * Platform Revenue are distinct business concepts (see the respective DTOs) and must never be
 * combined into a single figure.
 */
record RevenueExportData(
        RevenueReportResponse revenue,
        CommissionTimeSeriesResponse commission,
        PlatformRevenueSummaryResponse platformRevenue,
        OffsetDateTime generatedAt
) {}
