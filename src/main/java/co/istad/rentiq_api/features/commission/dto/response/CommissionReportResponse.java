package co.istad.rentiq_api.features.commission.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CommissionReportResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal totalCommission,
        long totalBookings,
        List<CategoryCommissionSummary> byCategory
) {}
