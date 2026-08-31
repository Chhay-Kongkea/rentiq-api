package co.istad.rentiq_api.features.notification.dto.response;

import co.istad.rentiq_api.features.notification.enums.NotificationReferenceType;
import co.istad.rentiq_api.features.notification.enums.NotificationType;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Admin operational-history view of a persisted Notification — adds the recipient userId,
 * which the self-service NotificationResponse omits (a user's own inbox never needs to be
 * told whose inbox it is). No delivery-channel fields: no NotificationDelivery model exists
 * in this codebase, so there is nothing further to expose.
 */
public record AdminNotificationResponse(
        UUID id,
        String userId,
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
) {}
