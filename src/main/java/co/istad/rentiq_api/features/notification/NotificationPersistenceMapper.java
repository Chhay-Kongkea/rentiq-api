package co.istad.rentiq_api.features.notification;

import co.istad.rentiq_api.features.notification.entity.Notification;
import co.istad.rentiq_api.features.notification.enums.NotificationReferenceType;
import co.istad.rentiq_api.features.notification.enums.NotificationType;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Compatibility boundary for the legacy PostgreSQL notification CHECK constraints.
 * Semantic event/reference values are copied to JSON metadata before entity fields are
 * normalized to values accepted by the existing database.
 */
public final class NotificationPersistenceMapper {

    public static final String EVENT_TYPE_KEY = "eventType";
    public static final String STATUS_KEY = "status";
    public static final String REFERENCE_TYPE_KEY = "referenceType";
    public static final String REFERENCE_ID_KEY = "referenceId";

    private static final Set<NotificationType> PERSISTED_TYPES = EnumSet.of(
            NotificationType.BOOKING,
            NotificationType.PAYMENT,
            NotificationType.ITEM_REQUEST,
            NotificationType.OFFER,
            NotificationType.ITEM,
            NotificationType.MARKETING,
            NotificationType.SYSTEM
    );

    private static final Set<NotificationReferenceType> PERSISTED_REFERENCE_TYPES = EnumSet.of(
            NotificationReferenceType.BOOKING,
            NotificationReferenceType.PAYMENT,
            NotificationReferenceType.ITEM_REQUEST,
            NotificationReferenceType.OFFER,
            NotificationReferenceType.ITEM,
            NotificationReferenceType.USER
    );

    private NotificationPersistenceMapper() {
    }

    public static Notification prepareForPersistence(Notification notification) {
        NotificationType semanticType = notification.getNotificationType();
        NotificationReferenceType semanticReferenceType = notification.getReferenceType();

        Map<String, Object> payload = notification.getPayload() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(notification.getPayload());

        if (semanticType != null) {
            payload.put(EVENT_TYPE_KEY, semanticType.name());
            notification.setNotificationType(resolveType(semanticType));
        }

        statusFromTitle(notification.getTitle()).ifPresent(status -> payload.putIfAbsent(STATUS_KEY, status));

        if (semanticReferenceType != null) {
            payload.put(REFERENCE_TYPE_KEY, semanticReferenceType.name());
            notification.setReferenceType(resolveReferenceType(semanticReferenceType));
        }

        if (notification.getReferenceId() != null) {
            payload.put(REFERENCE_ID_KEY, notification.getReferenceId().toString());
        }

        notification.setPayload(payload);
        return notification;
    }

    public static NotificationType resolveType(NotificationType semanticType) {
        return PERSISTED_TYPES.contains(semanticType) ? semanticType : NotificationType.SYSTEM;
    }

    public static NotificationReferenceType resolveReferenceType(NotificationReferenceType semanticReferenceType) {
        return PERSISTED_REFERENCE_TYPES.contains(semanticReferenceType) ? semanticReferenceType : null;
    }

    public static boolean isPersistedType(NotificationType type) {
        return type != null && PERSISTED_TYPES.contains(type);
    }

    public static boolean isPersistedReferenceType(NotificationReferenceType type) {
        return type != null && PERSISTED_REFERENCE_TYPES.contains(type);
    }

    public static NotificationType semanticType(Notification notification) {
        return enumMetadata(notification.getPayload(), EVENT_TYPE_KEY, NotificationType.class)
                .orElse(notification.getNotificationType());
    }

    public static NotificationReferenceType semanticReferenceType(Notification notification) {
        return enumMetadata(notification.getPayload(), REFERENCE_TYPE_KEY, NotificationReferenceType.class)
                .orElse(notification.getReferenceType());
    }

    private static <E extends Enum<E>> java.util.Optional<E> enumMetadata(
            Map<String, Object> payload,
            String key,
            Class<E> enumClass
    ) {
        if (payload == null || payload.get(key) == null) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(Enum.valueOf(enumClass, payload.get(key).toString()));
        } catch (IllegalArgumentException ignored) {
            return java.util.Optional.empty();
        }
    }

    private static java.util.Optional<String> statusFromTitle(String title) {
        if (title == null || title.isBlank()) {
            return java.util.Optional.empty();
        }
        String normalized = title.trim().toUpperCase(java.util.Locale.ROOT);
        for (String status : java.util.List.of(
                "APPROVED", "REJECTED", "SUSPENDED", "RESOLVED", "DISMISSED",
                "REVIEWED", "HIDDEN", "RESTORED", "CREDITED")) {
            if (normalized.endsWith(status)) {
                return java.util.Optional.of(status);
            }
        }
        return java.util.Optional.empty();
    }
}
