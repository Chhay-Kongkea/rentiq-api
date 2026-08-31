package co.istad.rentiq_api.features.notification.entity;

import co.istad.rentiq_api.features.notification.enums.NotificationReferenceType;
import co.istad.rentiq_api.features.notification.enums.NotificationType;
import co.istad.rentiq_api.features.notification.NotificationConstraints;
import co.istad.rentiq_api.features.notification.NotificationPersistenceMapper;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 80)
    private NotificationType notificationType;

    @Column(name = "title", nullable = false, length = NotificationConstraints.TITLE_MAX_LENGTH)
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> payload = new LinkedHashMap<>();

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", length = 50)
    private NotificationReferenceType referenceType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        assertPersistenceCompatible();
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }

        if (payload == null) {
            payload = new LinkedHashMap<>();
        }
    }

    @PreUpdate
    public void preUpdate() {
        assertPersistenceCompatible();
    }

    private void assertPersistenceCompatible() {
        if (notificationType != null && !NotificationPersistenceMapper.isPersistedType(notificationType)) {
            throw new IllegalStateException("Unsupported persisted notification type: " + notificationType);
        }
        if (referenceType != null && !NotificationPersistenceMapper.isPersistedReferenceType(referenceType)) {
            throw new IllegalStateException("Unsupported persisted notification reference type: " + referenceType);
        }
    }

    public void markAsRead() {
        if (!read) {
            read = true;
            readAt = OffsetDateTime.now();
        }
    }
}
