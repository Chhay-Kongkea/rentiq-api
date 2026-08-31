package co.istad.rentiq_api.features.commission.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoryCommissionSummary(
        UUID categoryId,
        String categoryName,
        BigDecimal totalCommission,
        long bookingCount
) {}
