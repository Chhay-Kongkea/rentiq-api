package co.istad.rentiq_api.features.financialReport.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CommissionTimeSeriesRow(
        LocalDate period,
        BigDecimal totalCommission,
        long bookingCount
) {}
