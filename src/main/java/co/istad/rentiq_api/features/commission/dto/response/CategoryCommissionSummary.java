package co.istad.rentiq_api.features.commission.dto.response;

import java.math.BigDecimal;

public record CategoryCommissionSummary(
        Integer categoryId,
        String categoryName,
        BigDecimal totalCommission,
        long bookingCount
) {}
