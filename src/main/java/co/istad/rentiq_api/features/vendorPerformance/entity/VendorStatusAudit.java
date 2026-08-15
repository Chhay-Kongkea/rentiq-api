package co.istad.rentiq_api.features.vendorPerformance.entity;

import co.istad.rentiq_api.features.vendorPerformance.enums.VendorModerationAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "vendor_status_audit")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorStatusAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "admin_id", nullable = false, length = 255)
    private String adminId;

    @Column(name = "target_id", nullable = false, length = 255)
    private String targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private VendorModerationAction action;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
