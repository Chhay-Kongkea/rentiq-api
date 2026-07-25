package co.istad.rentiq_api.features.userProfile.dto.response;


import lombok.Builder;

@Builder
public record NotificationPreferencesResponse(
        boolean bookingNotifications,
        boolean paymentNotifications,
        boolean marketingNotifications,
        boolean emailNotifications,
        boolean pushNotifications
) {}