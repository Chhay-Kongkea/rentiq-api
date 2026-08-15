package co.istad.rentiq_api.features.item.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table( name = "item_images",
        indexes = {
                @Index(name = "idx_item_images_item_id", columnList = "item_id"),
                @Index(name = "idx_item_images_sort_order", columnList = "item_id, sort_order")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_item_images_public_id", columnNames = "public_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false, foreignKey = @ForeignKey(name = "fk_item_images_item"))
    private Item item;

    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "public_id", nullable = false, unique = true, length = 500)
    private String publicId;

    @Column(name = "asset_id", length = 255)
    private String assetId;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean primary = false;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
        createdAt = OffsetDateTime.now();
    }

        if (sortOrder == null) {
            sortOrder = 0;
        }
    }
}