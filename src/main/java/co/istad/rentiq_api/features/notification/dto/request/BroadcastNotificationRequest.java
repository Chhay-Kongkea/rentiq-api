package co.istad.rentiq_api.features.notification.dto.request;

import co.istad.rentiq_api.features.notification.enums.NotificationReferenceType;
import co.istad.rentiq_api.features.notification.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record BroadcastNotificationRequest(

        @NotEmpty(message = "At least one recipient user ID is required")
        Set<@NotBlank(message = "Recipient user ID cannot be blank") String> userIds,

        @NotNull(message = "Notification type is required")
        NotificationType notificationType,

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title cannot exceed 200 characters")
        String title,

        @NotBlank(message = "Body is required")
        String body,

        Map<String, Object> payload,
        UUID referenceId,
        NotificationReferenceType referenceType

) {
}