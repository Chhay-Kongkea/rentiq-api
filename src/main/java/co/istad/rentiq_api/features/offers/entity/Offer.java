package co.istad.rentiq_api.features.offers.entity;


import co.istad.rentiq_api.features.item.entity.Item;
import co.istad.rentiq_api.features.offers.enums.OfferStatus;
import jakarta.persistence.*;
import lombok.*;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "offers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Offer {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String requesterId;

    @Column(nullable = false)
    private String vendorId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="item_id")
    private Item item;

    private BigDecimal offeredPrice;

    private String currency;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    private OfferStatus status;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist(){
        createdAt = OffsetDateTime.now();
        updatedAt = createdAt;

        if(status==null)
            status = OfferStatus.PENDING;

        if(currency==null)
            currency="USD";
    }

    @PreUpdate
    void preUpdate(){
        updatedAt = OffsetDateTime.now();
    }

}