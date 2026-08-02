package co.istad.rentiq_api.features.financialReport.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VendorEarningsPeriodRow(
        LocalDate period,
        BigDecimal totalEarnings,
        long transactionCount
) {}
