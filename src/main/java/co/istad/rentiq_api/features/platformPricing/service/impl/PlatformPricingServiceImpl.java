package co.istad.rentiq_api.features.platformPricing.service.impl;

import co.istad.rentiq_api.features.advertisement.enums.AdvertisementPackage;
import co.istad.rentiq_api.features.platformPricing.dto.response.PackagePricingResponse;
import co.istad.rentiq_api.features.platformPricing.dto.response.PlatformPricingResponse;
import co.istad.rentiq_api.features.platformPricing.service.PlatformPricingService;
import co.istad.rentiq_api.features.platformSetting.enums.PlatformSettingKey;
import co.istad.rentiq_api.features.platformSetting.service.PlatformSettingService;
import co.istad.rentiq_api.features.promotion.enums.PromotionPackage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Owns the only package+currency -> PlatformSettingKey mapping in the codebase. Promotion and
 * Advertisement services never build a setting-key string themselves.
 */
@Service
@RequiredArgsConstructor
public class PlatformPricingServiceImpl implements PlatformPricingService {

    private final PlatformSettingService platformSettingService;

    @Override
    public Optional<BigDecimal> getPromotionPrice(PromotionPackage packageType, String currency) {
        return Optional.ofNullable(promotionKey(packageType, currency))
                .map(platformSettingService::getEffectiveValue);
    }

    @Override
    public Optional<BigDecimal> getAdvertisementPrice(AdvertisementPackage packageType, String currency) {
        return Optional.ofNullable(advertisementKey(packageType, currency))
                .map(platformSettingService::getEffectiveValue);
    }

    @Override
    public PlatformPricingResponse getPublicPricing() {
        List<PackagePricingResponse> promotions = Arrays.stream(PromotionPackage.values())
                .map(this::toPromotionPricing)
                .toList();

        List<PackagePricingResponse> advertisements = Arrays.stream(AdvertisementPackage.values())
                .map(this::toAdvertisementPricing)
                .toList();

        return new PlatformPricingResponse(promotions, advertisements);
    }

    private PackagePricingResponse toPromotionPricing(PromotionPackage packageType) {
        Map<String, BigDecimal> prices = new LinkedHashMap<>();
        getPromotionPrice(packageType, "USD").ifPresent(price -> prices.put("USD", price));
        getPromotionPrice(packageType, "KHR").ifPresent(price -> prices.put("KHR", price));
        return new PackagePricingResponse(packageType.name(), packageType.getDurationDays(), prices);
    }

    private PackagePricingResponse toAdvertisementPricing(AdvertisementPackage packageType) {
        Map<String, BigDecimal> prices = new LinkedHashMap<>();
        getAdvertisementPrice(packageType, "USD").ifPresent(price -> prices.put("USD", price));
        getAdvertisementPrice(packageType, "KHR").ifPresent(price -> prices.put("KHR", price));
        return new PackagePricingResponse(packageType.name(), packageType.getDurationDays(), prices);
    }

    private PlatformSettingKey promotionKey(PromotionPackage packageType, String currency) {
        return switch (packageType) {
            case BOOST_1_DAY -> currencyKey(currency, PlatformSettingKey.PROMOTION_BOOST_1_DAY_USD, PlatformSettingKey.PROMOTION_BOOST_1_DAY_KHR);
            case BOOST_3_DAYS -> currencyKey(currency, PlatformSettingKey.PROMOTION_BOOST_3_DAYS_USD, PlatformSettingKey.PROMOTION_BOOST_3_DAYS_KHR);
            case BOOST_7_DAYS -> currencyKey(currency, PlatformSettingKey.PROMOTION_BOOST_7_DAYS_USD, PlatformSettingKey.PROMOTION_BOOST_7_DAYS_KHR);
        };
    }

    private PlatformSettingKey advertisementKey(AdvertisementPackage packageType, String currency) {
        return switch (packageType) {
            case AD_3_DAYS -> currencyKey(currency, PlatformSettingKey.ADVERTISEMENT_AD_3_DAYS_USD, PlatformSettingKey.ADVERTISEMENT_AD_3_DAYS_KHR);
            case AD_7_DAYS -> currencyKey(currency, PlatformSettingKey.ADVERTISEMENT_AD_7_DAYS_USD, PlatformSettingKey.ADVERTISEMENT_AD_7_DAYS_KHR);
            case AD_14_DAYS -> currencyKey(currency, PlatformSettingKey.ADVERTISEMENT_AD_14_DAYS_USD, PlatformSettingKey.ADVERTISEMENT_AD_14_DAYS_KHR);
        };
    }

    private PlatformSettingKey currencyKey(String currency, PlatformSettingKey usdKey, PlatformSettingKey khrKey) {
        if ("USD".equals(currency)) {
            return usdKey;
        }
        if ("KHR".equals(currency)) {
            return khrKey;
        }
        return null;
    }
}
