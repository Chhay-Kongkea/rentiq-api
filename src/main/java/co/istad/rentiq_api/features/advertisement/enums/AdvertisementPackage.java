package co.istad.rentiq_api.features.advertisement.enums;

/**
 * Package identity/duration ONLY — pricing is no longer carried here. As of the Admin Settings
 * + centralized pricing configuration task, the backend's pricing authority is
 * {@code PlatformPricingService} (which resolves an Admin-overridable {@code PlatformSetting},
 * falling back to a fixed default). Keeping duration here and price in settings avoids two
 * competing sources of truth: duration is fixed product logic, price is configuration.
 */
public enum AdvertisementPackage {

    AD_3_DAYS(3),
    AD_7_DAYS(7),
    AD_14_DAYS(14);

    private final int durationDays;

    AdvertisementPackage(int durationDays) {
        this.durationDays = durationDays;
    }

    public int getDurationDays() {
        return durationDays;
    }
}
