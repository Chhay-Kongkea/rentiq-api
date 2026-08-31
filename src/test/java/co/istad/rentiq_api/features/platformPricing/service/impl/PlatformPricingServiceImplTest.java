package co.istad.rentiq_api.features.platformPricing.service.impl;

import co.istad.rentiq_api.features.advertisement.enums.AdvertisementPackage;
import co.istad.rentiq_api.features.platformPricing.dto.response.PackagePricingResponse;
import co.istad.rentiq_api.features.platformPricing.dto.response.PlatformPricingResponse;
import co.istad.rentiq_api.features.platformSetting.enums.PlatformSettingKey;
import co.istad.rentiq_api.features.platformSetting.service.PlatformSettingService;
import co.istad.rentiq_api.features.promotion.enums.PromotionPackage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformPricingServiceImplTest {

    @Mock private PlatformSettingService platformSettingService;

    private PlatformPricingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PlatformPricingServiceImpl(platformSettingService);
    }

    @Test
    void getPromotionPrice_usd_resolvesCorrectKey() {
        when(platformSettingService.getEffectiveValue(PlatformSettingKey.PROMOTION_BOOST_7_DAYS_USD))
                .thenReturn(new BigDecimal("5.00"));

        Optional<BigDecimal> price = service.getPromotionPrice(PromotionPackage.BOOST_7_DAYS, "USD");

        assertThat(price).contains(new BigDecimal("5.00"));
    }

    @Test
    void getPromotionPrice_khr_resolvesCorrectKey() {
        when(platformSettingService.getEffectiveValue(PlatformSettingKey.PROMOTION_BOOST_1_DAY_KHR))
                .thenReturn(new BigDecimal("4000"));

        Optional<BigDecimal> price = service.getPromotionPrice(PromotionPackage.BOOST_1_DAY, "KHR");

        assertThat(price).contains(new BigDecimal("4000"));
    }

    @Test
    void getPromotionPrice_unsupportedCurrency_returnsEmpty_neverDefaultsToUsd() {
        Optional<BigDecimal> price = service.getPromotionPrice(PromotionPackage.BOOST_7_DAYS, "EUR");

        assertThat(price).isEmpty();
    }

    @Test
    void getAdvertisementPrice_usd_resolvesCorrectKey() {
        when(platformSettingService.getEffectiveValue(PlatformSettingKey.ADVERTISEMENT_AD_7_DAYS_USD))
                .thenReturn(new BigDecimal("6.00"));

        Optional<BigDecimal> price = service.getAdvertisementPrice(AdvertisementPackage.AD_7_DAYS, "USD");

        assertThat(price).contains(new BigDecimal("6.00"));
    }

    @Test
    void getAdvertisementPrice_khr_resolvesCorrectKey() {
        when(platformSettingService.getEffectiveValue(PlatformSettingKey.ADVERTISEMENT_AD_14_DAYS_KHR))
                .thenReturn(new BigDecimal("40000"));

        Optional<BigDecimal> price = service.getAdvertisementPrice(AdvertisementPackage.AD_14_DAYS, "KHR");

        assertThat(price).contains(new BigDecimal("40000"));
    }

    @Test
    void getAdvertisementPrice_unsupportedCurrency_returnsEmpty() {
        assertThat(service.getAdvertisementPrice(AdvertisementPackage.AD_3_DAYS, "EUR")).isEmpty();
    }

    @Test
    void getPublicPricing_reflectsOverride_immediately() {
        // Default 5.00, but an Admin override of 6.00 must be reflected without any code change.
        stubRemainingDefaults();
        when(platformSettingService.getEffectiveValue(PlatformSettingKey.PROMOTION_BOOST_7_DAYS_USD))
                .thenReturn(new BigDecimal("6.00"));

        PlatformPricingResponse response = service.getPublicPricing();

        PackagePricingResponse boost7 = response.promotions().stream()
                .filter(p -> p.packageType().equals("BOOST_7_DAYS")).findFirst().orElseThrow();
        assertThat(boost7.durationDays()).isEqualTo(7);
        assertThat(boost7.prices().get("USD")).isEqualByComparingTo("6.00");
    }

    @Test
    void getPublicPricing_includesAllPackages_withCorrectDurations() {
        stubRemainingDefaults();

        PlatformPricingResponse response = service.getPublicPricing();

        assertThat(response.promotions()).hasSize(3);
        assertThat(response.advertisements()).hasSize(3);
        assertThat(response.advertisements().stream().map(PackagePricingResponse::packageType))
                .containsExactlyInAnyOrder("AD_3_DAYS", "AD_7_DAYS", "AD_14_DAYS");
    }

    /** Stubs every key except the one under test with its own default — keeps each test focused. */
    private void stubRemainingDefaults() {
        for (PlatformSettingKey key : PlatformSettingKey.values()) {
            when(platformSettingService.getEffectiveValue(key)).thenReturn(key.getDefaultValue());
        }
    }
}
