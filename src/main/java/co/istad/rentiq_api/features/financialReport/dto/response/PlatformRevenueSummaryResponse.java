package co.istad.rentiq_api.features.financialReport.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Platform Revenue = successful PROMOTION + ADVERTISEMENT OUT WalletTransaction charges only.
 * Never a combined cross-currency total — see {@code currencies}, one entry per supported
 * currency (USD, KHR), always present even when zero, never merged into a single number.
 */
public record PlatformRevenueSummaryResponse(
        OffsetDateTime from,
        OffsetDateTime to,
        List<PlatformRevenueCurrencySummary> currencies
) {}
