package co.istad.rentiq_api.features.promotion.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PromotionPackage is duration/identity only after the Admin Settings + centralized pricing
 * task — pricing now lives in PlatformPricingService (see PlatformPricingServiceImplTest).
 */
class PromotionPackageTest {

    @Test
    void boost1Day_duration() {
        assertThat(PromotionPackage.BOOST_1_DAY.getDurationDays()).isEqualTo(1);
    }

    @Test
    void boost3Days_duration() {
        assertThat(PromotionPackage.BOOST_3_DAYS.getDurationDays()).isEqualTo(3);
    }

    @Test
    void boost7Days_duration() {
        assertThat(PromotionPackage.BOOST_7_DAYS.getDurationDays()).isEqualTo(7);
    }
}
