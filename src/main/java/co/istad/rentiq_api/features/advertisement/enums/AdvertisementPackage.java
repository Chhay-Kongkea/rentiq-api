package co.istad.rentiq_api.features.advertisement.enums;

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
