package co.istad.rentiq_api.features.financialReport.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record PlatformRevenueBreakdownResponse(
        OffsetDateTime from,
        OffsetDateTime to,
        List<PlatformRevenueCurrencyBreakdown> currencies
) {}
