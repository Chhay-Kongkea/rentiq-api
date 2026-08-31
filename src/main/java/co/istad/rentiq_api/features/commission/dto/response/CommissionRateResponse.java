package co.istad.rentiq_api.features.commission.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * commissionRate is a fraction (0.1000 == 10%), matching categories.commission_rate.
 */
public record CommissionRateResponse(
        UUID categoryId,
        String categoryName,
        String categorySlug,
        BigDecimal commissionRate
) {}
