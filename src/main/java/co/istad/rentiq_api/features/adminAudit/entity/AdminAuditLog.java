package co.istad.rentiq_api.features.adminAudit.entity;

import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditPersistedAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditPersistedTargetType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Centralized, append-only record of successful admin actions. Never written to directly by
 * controllers — only AdminAuditServiceImpl persists rows, always after the business mutation
 * it describes has already succeeded.
 */
@Entity
@Table(
        name = "admin_audit_logs",
        indexes = {
                @Index(name = "idx_admin_audit_logs_created_at", columnList = "created_at"),
                @Index(name = "idx_admin_audit_logs_admin_id", columnList = "admin_id"),
                @Index(name = "idx_admin_audit_logs_action", columnList = "action"),
                @Index(name = "idx_admin_audit_logs_target_type", columnList = "target_type"),
                @Index(name = "idx_admin_audit_logs_target", columnList = "target_type, target_id"),
                @Index(name = "idx_admin_audit_logs_admin_created", columnList = "admin_id, created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "admin_id", nullable = false, length = 255)
    private String adminId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private AdminAuditPersistedAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private AdminAuditPersistedTargetType targetType;

    // String rather than UUID: target ids span a User's Keycloak subject id (String),
    // JPA-entity UUIDs (VendorApplication, Item, Booking, ...), and a Category's Integer id.
    @Column(name = "target_id", nullable = false, length = 255)
    private String targetId;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value", columnDefinition = "jsonb")
    private Map<String, Object> oldValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value", columnDefinition = "jsonb")
    private Map<String, Object> newValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
