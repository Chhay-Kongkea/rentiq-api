package co.istad.rentiq_api.features.advertisement.entity;

import co.istad.rentiq_api.features.advertisement.enums.AdvertisementPackage;
import co.istad.rentiq_api.features.advertisement.enums.AdvertisementStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * References the vendor and the advertised item by id (not a JPA relationship) — same
 * cross-feature convention used by WalletTransaction/AdminAuditLog elsewhere in this codebase.
 */
@Entity
@Table(
        name = "advertisements",
        indexes = {
                @Index(name = "idx_advertisements_vendor_id", columnList = "vendor_id"),
                @Index(name = "idx_advertisements_item_id", columnList = "item_id"),
                @Index(name = "idx_advertisements_status", columnList = "status"),
                @Index(name = "idx_advertisements_window", columnList = "start_at, end_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Advertisement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vendor_id", nullable = false, length = 255)
    private String vendorId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_type", length = 20)
    private AdvertisementPackage packageType;

    @Column(name = "duration_days")
    private Integer durationDays;

    /**
     * The price quote generated at submission/resubmission time from the CURRENT effective
     * setting — frozen so a later Admin price change never silently reprices a submission
     * already awaiting review. See {@code price}/{@code currency} for what was actually charged.
     */
    @Column(name = "quoted_price", precision = 15, scale = 2)
    private BigDecimal quotedPrice;

    @Column(name = "quoted_currency", length = 10)
    private String quotedCurrency;

    @Column(name = "quoted_at")
    private OffsetDateTime quotedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AdvertisementStatus status = AdvertisementStatus.PENDING;

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private OffsetDateTime endAt;

    /**
     * Frozen only at successful admin approval — the actual amount debited from the vendor's
     * wallet at that moment. Null while PENDING (or after a REJECTED resubmission, since a
     * rejected advertisement was never charged): this must never look like a completed payment
     * before one has actually happened.
     */
    @Column(name = "price", precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "reviewed_by", length = 255)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

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
            status = AdvertisementStatus.PENDING;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
