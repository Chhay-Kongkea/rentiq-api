package co.istad.rentiq_api.features.itemrequest.entity;

import co.istad.rentiq_api.features.item.entity.Item;
import co.istad.rentiq_api.features.itemrequest.enums.OfferStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "offers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "request_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_offers_request")
    )
    private ItemRequest itemRequest;

    @Column(name = "owner_id", nullable = false, length = 255)
    private String ownerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "item_id",
            foreignKey = @ForeignKey(name = "fk_offers_item")
    )
    private Item item;

    @Column(name = "offered_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal offeredPrice;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private OfferStatus status = OfferStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();

        currency = normalizeCurrency(currency);

        if (status == null) {
            status = OfferStatus.PENDING;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        currency = normalizeCurrency(currency);
        updatedAt = OffsetDateTime.now();
    }

    private String normalizeCurrency(String value) {
        if (value == null || value.isBlank()) {
            return "USD";
        }

        return value.trim().toUpperCase();
    }
}