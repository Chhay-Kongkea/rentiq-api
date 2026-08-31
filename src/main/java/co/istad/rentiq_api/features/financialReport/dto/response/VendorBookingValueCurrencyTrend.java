package co.istad.rentiq_api.features.financialReport.dto.response;

import java.util.List;

public record VendorBookingValueCurrencyTrend(
        String currency,
        List<VendorBookingValuePeriodPoint> points
) {}
