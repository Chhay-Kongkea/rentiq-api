package co.istad.rentiq_api.features.advertisement.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AdvertisementPackage is duration/identity only after the Admin Settings + centralized pricing
 * task — pricing now lives in PlatformPricingService (see PlatformPricingServiceImplTest).
 */
class AdvertisementPackageTest {

    @Test
    void ad3Days_duration() {
        assertThat(AdvertisementPackage.AD_3_DAYS.getDurationDays()).isEqualTo(3);
    }

    @Test
    void ad7Days_duration() {
        assertThat(AdvertisementPackage.AD_7_DAYS.getDurationDays()).isEqualTo(7);
    }

    @Test
    void ad14Days_duration() {
        assertThat(AdvertisementPackage.AD_14_DAYS.getDurationDays()).isEqualTo(14);
    }
}
