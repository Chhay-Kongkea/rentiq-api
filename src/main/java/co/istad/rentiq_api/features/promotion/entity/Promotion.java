package co.istad.rentiq_api.features.promotion.entity;

import co.istad.rentiq_api.features.promotion.enums.PromotionPackage;
import co.istad.rentiq_api.features.promotion.enums.PromotionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * References the vendor and the boosted item by id (not a JPA relationship) — same
 * cross-feature convention used by Advertisement/WalletTransaction/AdminAuditLog.
 *
 * price/currency/durationDays are frozen at purchase time: if PromotionPackage's pricing
 * changes later, historical Promotion rows must keep showing what was actually charged.
 */
@Entity
@Table(
        name = "promotions",
        indexes = {
                @Index(name = "idx_promotions_vendor_id", columnList = "vendor_id"),
                @Index(name = "idx_promotions_item_id", columnList = "item_id"),
                @Index(name = "idx_promotions_status", columnList = "status"),
                @Index(name = "idx_promotions_window", columnList = "start_at, end_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vendor_id", nullable = false, length = 255)
    private String vendorId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_type", nullable = false, length = 20)
    private PromotionPackage packageType;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PromotionStatus status = PromotionStatus.ACTIVE;

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private OffsetDateTime endAt;

    @Column(name = "impression_count", nullable = false)
    @Builder.Default
    private long impressionCount = 0L;

    @Column(name = "click_count", nullable = false)
    @Builder.Default
    private long clickCount = 0L;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "suspended_by", length = 255)
    private String suspendedBy;

    @Column(name = "suspended_at")
    private OffsetDateTime suspendedAt;

    @Column(name = "suspension_reason", columnDefinition = "TEXT")
    private String suspensionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = PromotionStatus.ACTIVE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
