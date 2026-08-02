package co.istad.rentiq_api.features.financialReport.dto.response;

import co.istad.rentiq_api.features.financialReport.dto.GroupBy;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CommissionTimeSeriesResponse(
        LocalDate from,
        LocalDate to,
        GroupBy groupBy,
        BigDecimal totalCommission,
        long totalBookings,
        Page<CommissionTimeSeriesRow> rows
) {}
