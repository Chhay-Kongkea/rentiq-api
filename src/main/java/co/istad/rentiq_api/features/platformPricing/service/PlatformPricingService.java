package co.istad.rentiq_api.features.platformPricing.service;

import co.istad.rentiq_api.features.advertisement.enums.AdvertisementPackage;
import co.istad.rentiq_api.features.platformPricing.dto.response.PlatformPricingResponse;
import co.istad.rentiq_api.features.promotion.enums.PromotionPackage;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * The single runtime pricing authority. PromotionServiceImpl and AdvertisementServiceImpl never
 * construct a PlatformSettingKey or read PlatformSettingService directly — every package+currency
 * price resolution goes through here, and the same resolution backs the public pricing endpoint.
 * Empty Optional means the currency isn't priced for that package (caller must reject the
 * purchase/quote with its own business exception — never fall back to USD, never convert).
 */
public interface PlatformPricingService {

    Optional<BigDecimal> getPromotionPrice(PromotionPackage packageType, String currency);

    Optional<BigDecimal> getAdvertisementPrice(AdvertisementPackage packageType, String currency);

    PlatformPricingResponse getPublicPricing();
}
