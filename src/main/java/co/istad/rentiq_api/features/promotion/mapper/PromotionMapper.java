package co.istad.rentiq_api.features.promotion.mapper;

import co.istad.rentiq_api.features.promotion.dto.response.PromotionResponse;
import co.istad.rentiq_api.features.promotion.dto.response.PromotionStatsResponse;
import co.istad.rentiq_api.features.promotion.entity.Promotion;
import co.istad.rentiq_api.features.promotion.enums.PromotionStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PromotionMapper {

    @Mapping(target = "status", expression = "java(effectiveStatus(promotion))")
    PromotionResponse toResponse(Promotion promotion);

    @Mapping(target = "promotionId", source = "id")
    @Mapping(target = "status", expression = "java(effectiveStatus(promotion))")
    @Mapping(target = "impressions", source = "impressionCount")
    @Mapping(target = "clicks", source = "clickCount")
    @Mapping(target = "ctr", expression = "java(computeCtr(promotion))")
    PromotionStatsResponse toStatsResponse(Promotion promotion);

    /**
     * An ACTIVE row whose window has already passed is reported as EXPIRED — this is the same
     * "effective status" rule the repository queries and search ranking use, so the API never
     * shows a lapsed promotion as still active just because no scheduler has rewritten it yet.
     */
    default PromotionStatus effectiveStatus(Promotion promotion) {
        if (promotion.getStatus() == PromotionStatus.ACTIVE && !promotion.getEndAt().isAfter(OffsetDateTime.now())) {
            return PromotionStatus.EXPIRED;
        }
        return promotion.getStatus();
    }

    default BigDecimal computeCtr(Promotion promotion) {
        if (promotion.getImpressionCount() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(promotion.getClickCount())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(promotion.getImpressionCount()), 2, RoundingMode.HALF_UP);
    }
}
