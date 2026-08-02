package co.istad.rentiq_api.features.category.cateogryDto;

import java.math.BigDecimal;

public record CategoryResponse(
        Integer id,
        Integer parentId,
        String name,
        String slug,
        BigDecimal commissionRate,
        String iconUrl,
        Boolean active
) {
}