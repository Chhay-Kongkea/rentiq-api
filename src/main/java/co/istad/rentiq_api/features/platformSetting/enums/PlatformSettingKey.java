package co.istad.rentiq_api.features.platformSetting.enums;

import java.math.BigDecimal;

/** Closed catalog of settings accepted by the database and Admin API. */
public enum PlatformSettingKey {
    ADVERTISEMENT_AD_3_DAYS_USD(SettingCategory.ADVERTISEMENT, "USD", "3.00"),
    ADVERTISEMENT_AD_3_DAYS_KHR(SettingCategory.ADVERTISEMENT, "KHR", "12000"),
    ADVERTISEMENT_AD_7_DAYS_USD(SettingCategory.ADVERTISEMENT, "USD", "6.00"),
    ADVERTISEMENT_AD_7_DAYS_KHR(SettingCategory.ADVERTISEMENT, "KHR", "24000"),
    ADVERTISEMENT_AD_14_DAYS_USD(SettingCategory.ADVERTISEMENT, "USD", "10.00"),
    ADVERTISEMENT_AD_14_DAYS_KHR(SettingCategory.ADVERTISEMENT, "KHR", "40000"),
    PROMOTION_BOOST_1_DAY_USD(SettingCategory.PROMOTION, "USD", "1.00"),
    PROMOTION_BOOST_1_DAY_KHR(SettingCategory.PROMOTION, "KHR", "4000"),
    PROMOTION_BOOST_3_DAYS_USD(SettingCategory.PROMOTION, "USD", "2.50"),
    PROMOTION_BOOST_3_DAYS_KHR(SettingCategory.PROMOTION, "KHR", "10000"),
    PROMOTION_BOOST_7_DAYS_USD(SettingCategory.PROMOTION, "USD", "5.00"),
    PROMOTION_BOOST_7_DAYS_KHR(SettingCategory.PROMOTION, "KHR", "20000"),
    PLATFORM_DISPLAY_NAME(SettingCategory.GENERAL, SettingValueType.STRING, "Rentiq",
            "Platform Display Name", "Presentation name shown to users"),
    SUPPORT_EMAIL(SettingCategory.GENERAL, SettingValueType.EMAIL, "support@rentiq.site",
            "Support Email", "Public support contact email"),
    DEFAULT_LOCALE(SettingCategory.GENERAL, SettingValueType.LOCALE, "en",
            "Default Locale", "Default locale when no user preference exists"),
    BOOKING_MAX_RENTAL_DAYS(SettingCategory.BOOKING, SettingValueType.INTEGER, "30", "1", "365",
            "Maximum Rental Duration", "Maximum allowed booking duration in days"),
    LISTING_MAX_IMAGES(SettingCategory.LISTING, SettingValueType.INTEGER, "8", "1", "20",
            "Maximum Images Per Listing", "Maximum images allowed for future listing uploads"),
    MARKETING_BROADCAST_ENABLED(SettingCategory.NOTIFICATION, SettingValueType.BOOLEAN, "1", "0", "1",
            "Marketing Broadcasts", "Controls optional Admin marketing broadcasts");

    private final SettingCategory category;
    private final SettingValueType valueType;
    private final String currency;
    private final BigDecimal defaultValue;
    private final String defaultTextValue;
    private final BigDecimal minimumValue;
    private final BigDecimal maximumValue;
    private final String label;
    private final String description;

    PlatformSettingKey(SettingCategory category, String currency, String defaultValue) {
        this.category = category; this.valueType = SettingValueType.DECIMAL; this.currency = currency;
        this.defaultValue = new BigDecimal(defaultValue); this.defaultTextValue = null;
        this.minimumValue = BigDecimal.ZERO; this.maximumValue = null;
        this.label = name().replace('_', ' '); this.description = "Configurable platform package price";
    }
    PlatformSettingKey(SettingCategory category, SettingValueType type, String textDefault,
                       String label, String description) {
        this.category = category; this.valueType = type; this.currency = null;
        this.defaultValue = null; this.defaultTextValue = textDefault;
        this.minimumValue = null; this.maximumValue = null; this.label = label; this.description = description;
    }
    PlatformSettingKey(SettingCategory category, SettingValueType type, String numericDefault,
                       String minimum, String maximum, String label, String description) {
        this.category = category; this.valueType = type; this.currency = null;
        this.defaultValue = new BigDecimal(numericDefault); this.defaultTextValue = null;
        this.minimumValue = new BigDecimal(minimum); this.maximumValue = new BigDecimal(maximum);
        this.label = label; this.description = description;
    }
    public SettingCategory getCategory() { return category; }
    public SettingValueType getValueType() { return valueType; }
    public String getCurrency() { return currency; }
    public BigDecimal getDefaultValue() { return defaultValue; }
    public String getDefaultTextValue() { return defaultTextValue; }
    public BigDecimal getMinimumValue() { return minimumValue; }
    public BigDecimal getMaximumValue() { return maximumValue; }
    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public boolean isTextual() {
        return valueType == SettingValueType.STRING || valueType == SettingValueType.EMAIL
                || valueType == SettingValueType.LOCALE;
    }
}
