package co.istad.rentiq_api.features.notification.dto.response;

import co.istad.rentiq_api.features.notification.enums.NotificationReferenceType;
import co.istad.rentiq_api.features.notification.enums.NotificationType;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType notificationType,
        String title,
        String body,
        Map<String, Object> payload,
        boolean read,
        OffsetDateTime readAt,
        UUID referenceId,
        NotificationReferenceType referenceType,
        OffsetDateTime createdAt,
        NotificationType eventType,
        NotificationReferenceType eventReferenceType
) {
}
