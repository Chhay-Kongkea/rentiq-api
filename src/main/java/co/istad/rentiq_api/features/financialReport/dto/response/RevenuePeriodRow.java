package co.istad.rentiq_api.features.financialReport.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenuePeriodRow(
        LocalDate period,
        BigDecimal totalRevenue,
        long bookingCount
) {}
