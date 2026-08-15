package co.istad.rentiq_api.features.category.cateogryDto;

import java.math.BigDecimal;

public record CategoryRequest(
        Integer parentId,
        String name,
        String slug,
        BigDecimal commissionRate,
        String iconUrl,
        Boolean active
) {
}