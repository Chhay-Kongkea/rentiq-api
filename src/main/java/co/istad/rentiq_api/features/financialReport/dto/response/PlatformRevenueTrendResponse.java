package co.istad.rentiq_api.features.financialReport.dto.response;

import co.istad.rentiq_api.features.financialReport.dto.GroupBy;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Missing periods within [from, to) are zero-filled per currency, so the UI never has to
 * handle gaps — see FinancialReportServiceImpl.getPlatformRevenueTrend.
 */
public record PlatformRevenueTrendResponse(
        OffsetDateTime from,
        OffsetDateTime to,
        GroupBy groupBy,
        List<PlatformRevenueCurrencyTrend> currencies
) {}
