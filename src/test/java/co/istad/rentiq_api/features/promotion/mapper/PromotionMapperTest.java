package co.istad.rentiq_api.features.promotion.mapper;

import co.istad.rentiq_api.features.promotion.entity.Promotion;
import co.istad.rentiq_api.features.promotion.enums.PromotionPackage;
import co.istad.rentiq_api.features.promotion.enums.PromotionStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PromotionMapperTest {

    // Default methods only — no generated implementation needed for this logic.
    private final PromotionMapper mapper = new PromotionMapperImpl();

    private Promotion promotion(PromotionStatus status, OffsetDateTime startAt, OffsetDateTime endAt) {
        return Promotion.builder()
                .id(UUID.randomUUID())
                .vendorId("vendor-1")
                .itemId(UUID.randomUUID())
                .packageType(PromotionPackage.BOOST_7_DAYS)
                .durationDays(7)
                .price(new BigDecimal("5.00"))
                .currency("USD")
                .status(status)
                .startAt(startAt)
                .endAt(endAt)
                .impressionCount(0)
                .clickCount(0)
                .build();
    }

    @Test
    void effectiveStatus_activeBeforeEndAt_staysActive() {
        Promotion promotion = promotion(PromotionStatus.ACTIVE, OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1));
        assertThat(mapper.effectiveStatus(promotion)).isEqualTo(PromotionStatus.ACTIVE);
    }

    @Test
    void effectiveStatus_activeAfterEndAt_reportsExpired() {
        Promotion promotion = promotion(PromotionStatus.ACTIVE, OffsetDateTime.now().minusDays(10), OffsetDateTime.now().minusDays(1));
        assertThat(mapper.effectiveStatus(promotion)).isEqualTo(PromotionStatus.EXPIRED);
    }

    @Test
    void effectiveStatus_cancelledStaysCancelled_regardlessOfWindow() {
        Promotion promotion = promotion(PromotionStatus.CANCELLED, OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(5));
        assertThat(mapper.effectiveStatus(promotion)).isEqualTo(PromotionStatus.CANCELLED);
    }

    @Test
    void computeCtr_zeroImpressions_isZero() {
        Promotion promotion = promotion(PromotionStatus.ACTIVE, OffsetDateTime.now(), OffsetDateTime.now().plusDays(1));
        promotion.setImpressionCount(0);
        promotion.setClickCount(0);
        assertThat(mapper.computeCtr(promotion)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void computeCtr_nonZero_computesPercentageToTwoDecimals() {
        Promotion promotion = promotion(PromotionStatus.ACTIVE, OffsetDateTime.now(), OffsetDateTime.now().plusDays(1));
        promotion.setImpressionCount(1240);
        promotion.setClickCount(86);
        // 86 / 1240 * 100 = 6.935483... -> rounds to 6.94
        assertThat(mapper.computeCtr(promotion)).isEqualByComparingTo("6.94");
    }

    /**
     * Minimal hand-written stand-in for the MapStruct-generated implementation — only the
     * default methods under test are exercised, so the two abstract mappings just need bodies.
     */
    private static class PromotionMapperImpl implements PromotionMapper {
        @Override
        public co.istad.rentiq_api.features.promotion.dto.response.PromotionResponse toResponse(Promotion promotion) {
            throw new UnsupportedOperationException("not needed for this test");
        }

        @Override
        public co.istad.rentiq_api.features.promotion.dto.response.PromotionStatsResponse toStatsResponse(Promotion promotion) {
            throw new UnsupportedOperationException("not needed for this test");
        }
    }
}
