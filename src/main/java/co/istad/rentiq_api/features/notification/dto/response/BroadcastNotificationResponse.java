package co.istad.rentiq_api.features.notification.dto.response;

public record BroadcastNotificationResponse(
        int recipientCount,
        int notificationCount
) {
}
