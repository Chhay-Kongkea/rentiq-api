package co.istad.rentiq_api.features.platformSetting.enums;

import java.math.BigDecimal;

/**
 * The complete, closed set of Admin-configurable platform pricing settings — Admin Settings v1
 * supports ONLY these twelve monetary keys, never an arbitrary Admin-invented key. Each key
 * carries its own static metadata (category, currency, default value); the database only ever
 * stores an override row when an Admin has actually changed a price (see PlatformSettingService)
 * — defaultValue here is what every environment behaves as with an empty settings table, which
 * is what keeps this feature deployable without a seed migration.
 */
public enum PlatformSettingKey {

    ADVERTISEMENT_AD_3_DAYS_USD(SettingCategory.ADVERTISEMENT, "USD", new BigDecimal("3.00")),
    ADVERTISEMENT_AD_3_DAYS_KHR(SettingCategory.ADVERTISEMENT, "KHR", new BigDecimal("12000")),
    ADVERTISEMENT_AD_7_DAYS_USD(SettingCategory.ADVERTISEMENT, "USD", new BigDecimal("6.00")),
    ADVERTISEMENT_AD_7_DAYS_KHR(SettingCategory.ADVERTISEMENT, "KHR", new BigDecimal("24000")),
    ADVERTISEMENT_AD_14_DAYS_USD(SettingCategory.ADVERTISEMENT, "USD", new BigDecimal("10.00")),
    ADVERTISEMENT_AD_14_DAYS_KHR(SettingCategory.ADVERTISEMENT, "KHR", new BigDecimal("40000")),

    PROMOTION_BOOST_1_DAY_USD(SettingCategory.PROMOTION, "USD", new BigDecimal("1.00")),
    PROMOTION_BOOST_1_DAY_KHR(SettingCategory.PROMOTION, "KHR", new BigDecimal("4000")),
    PROMOTION_BOOST_3_DAYS_USD(SettingCategory.PROMOTION, "USD", new BigDecimal("2.50")),
    PROMOTION_BOOST_3_DAYS_KHR(SettingCategory.PROMOTION, "KHR", new BigDecimal("10000")),
    PROMOTION_BOOST_7_DAYS_USD(SettingCategory.PROMOTION, "USD", new BigDecimal("5.00")),
    PROMOTION_BOOST_7_DAYS_KHR(SettingCategory.PROMOTION, "KHR", new BigDecimal("20000"));

    private final SettingCategory category;
    private final String currency;
    private final BigDecimal defaultValue;

    PlatformSettingKey(SettingCategory category, String currency, BigDecimal defaultValue) {
        this.category = category;
        this.currency = currency;
        this.defaultValue = defaultValue;
    }

    public SettingCategory getCategory() {
        return category;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getDefaultValue() {
        return defaultValue;
    }
}
