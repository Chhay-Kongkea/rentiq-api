package co.istad.rentiq_api.features.financialReport.dto.response;

import java.math.BigDecimal;

public record VendorBookingValuePeriodPoint(
        String period,
        BigDecimal completedBookingValue,
        long completedBookingCount
) {}
