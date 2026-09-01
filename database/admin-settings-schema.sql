BEGIN;

ALTER TABLE platform_settings
    ADD COLUMN IF NOT EXISTS text_value TEXT;

ALTER TABLE platform_settings
    ALTER COLUMN value DROP NOT NULL;

ALTER TABLE platform_settings
    DROP CONSTRAINT IF EXISTS platform_settings_setting_key_check;

ALTER TABLE platform_settings
    ADD CONSTRAINT platform_settings_setting_key_check CHECK (
        setting_key IN (
            'ADVERTISEMENT_AD_3_DAYS_USD',
            'ADVERTISEMENT_AD_3_DAYS_KHR',
            'ADVERTISEMENT_AD_7_DAYS_USD',
            'ADVERTISEMENT_AD_7_DAYS_KHR',
            'ADVERTISEMENT_AD_14_DAYS_USD',
            'ADVERTISEMENT_AD_14_DAYS_KHR',
            'PROMOTION_BOOST_1_DAY_USD',
            'PROMOTION_BOOST_1_DAY_KHR',
            'PROMOTION_BOOST_3_DAYS_USD',
            'PROMOTION_BOOST_3_DAYS_KHR',
            'PROMOTION_BOOST_7_DAYS_USD',
            'PROMOTION_BOOST_7_DAYS_KHR',
            'BOOKING_MAX_RENTAL_DAYS',
            'LISTING_MAX_IMAGES',
            'MARKETING_BROADCAST_ENABLED',
            'PLATFORM_DISPLAY_NAME',
            'SUPPORT_EMAIL',
            'DEFAULT_LOCALE'
        )
    );

ALTER TABLE platform_settings
    DROP CONSTRAINT IF EXISTS platform_settings_value_representation_check;

ALTER TABLE platform_settings
    ADD CONSTRAINT platform_settings_value_representation_check CHECK (
        (value IS NOT NULL AND text_value IS NULL)
        OR (value IS NULL AND text_value IS NOT NULL)
    );

COMMIT;
