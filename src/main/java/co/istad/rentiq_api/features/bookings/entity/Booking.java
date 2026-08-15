package co.istad.rentiq_api.features.bookings.entity;

import co.istad.rentiq_api.features.bookings.enums.BookingStatus;
import co.istad.rentiq_api.features.bookings.enums.PaymentStatus;
import co.istad.rentiq_api.features.item.entity.Item;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "bookings",
        indexes = {
                @Index(name = "idx_bookings_status", columnList = "status"),
                @Index(name = "idx_bookings_customer", columnList = "customer_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_ref", unique = true, nullable = false, length = 20)
    private String bookingRef;

    @Column(name = "customer_id", nullable = false, length = 255)
    private String customerId;

    @Column(name = "owner_id", nullable = false, length = 255)
    private String ownerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "offer_id")
    private UUID offerId;

    @Column(name = "rental_start", nullable = false)
    private LocalDate rentalStart;

    @Column(name = "rental_end", nullable = false)
    private LocalDate rentalEnd;

    @Column(name = "rental_days", nullable = false)
    private Short rentalDays;

    @Column(name = "booked_price_per_day", nullable = false, precision = 15, scale = 2)
    private BigDecimal bookedPricePerDay;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "security_deposit", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal securityDeposit = BigDecimal.ZERO;

    @Column(name = "commission_rate", precision = 5, scale = 4)
    private BigDecimal commissionRate;

    @Column(name = "commission_amount", precision = 15, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "owner_confirmed_at")
    private OffsetDateTime ownerConfirmedAt;

    @Column(name = "security_deposit_returned_at")
    private OffsetDateTime securityDepositReturnedAt;

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
            status = BookingStatus.PENDING;
        }

        if (paymentStatus == null) {
            paymentStatus = PaymentStatus.UNPAID;
        }

        if (currency == null) {
            currency = "USD";
        }

        if (securityDeposit == null) {
            securityDeposit = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
