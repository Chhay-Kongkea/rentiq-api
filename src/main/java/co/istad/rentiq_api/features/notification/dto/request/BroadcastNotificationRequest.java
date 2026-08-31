package co.istad.rentiq_api.features.notification.dto.request;

import co.istad.rentiq_api.features.notification.enums.NotificationReferenceType;
import co.istad.rentiq_api.features.notification.enums.NotificationType;
import co.istad.rentiq_api.features.notification.enums.BroadcastAudienceType;
import co.istad.rentiq_api.features.notification.NotificationConstraints;
import co.istad.rentiq_api.features.notification.NotificationPersistenceMapper;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public record BroadcastNotificationRequest(

        @NotNull(message = "Audience type is required")
        BroadcastAudienceType audienceType,

        String userId,

        @NotNull(message = "Notification type is required")
        NotificationType notificationType,

        @NotBlank(message = "Title is required")
        @Size(max = NotificationConstraints.TITLE_MAX_LENGTH, message = "Title cannot exceed 20 characters")
        String title,

        @NotBlank(message = "Body is required")
        String body,

        Map<String, Object> payload,
        UUID referenceId,
        NotificationReferenceType referenceType

) {
    @AssertTrue(message = "userId is required for SINGLE_USER broadcasts")
    public boolean isAudienceValid() {
        return audienceType == null
                || audienceType == BroadcastAudienceType.ALL_USERS
                || (userId != null && !userId.isBlank());
    }

    @AssertTrue(message = "Broadcast notification type must be MARKETING or SYSTEM")
    public boolean isBroadcastTypeSupported() {
        return notificationType == null
                || notificationType == NotificationType.MARKETING
                || notificationType == NotificationType.SYSTEM;
    }

    @AssertTrue(message = "Broadcast reference type must be supported by the current database")
    public boolean isReferenceTypeSupported() {
        return referenceType == null || NotificationPersistenceMapper.isPersistedReferenceType(referenceType);
    }
}
