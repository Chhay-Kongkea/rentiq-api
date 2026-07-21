package co.istad.rentiq_api.features.userProfile.dto.request;

public record NotificationPreferencesRequest(
        Boolean bookingNotifications,
        Boolean paymentNotifications,
        Boolean marketingNotifications,
        Boolean emailNotifications,
        Boolean pushNotifications
) {}