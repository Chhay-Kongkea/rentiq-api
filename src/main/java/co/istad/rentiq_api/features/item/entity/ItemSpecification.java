package co.istad.rentiq_api.features.item.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "item_specifications",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_item_specification_key",
                        columnNames = {
                                "item_id",
                                "spec_key"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_item_specifications_item",
                        columnList = "item_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemSpecification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_item_specifications_item"
            )
    )
    private Item item;

    @Column(name = "spec_key", nullable = false, length = 100)
    private String key;

    @Column(name = "spec_value", nullable = false, length = 500)
    private String value;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (sortOrder == null) {
            sortOrder = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}