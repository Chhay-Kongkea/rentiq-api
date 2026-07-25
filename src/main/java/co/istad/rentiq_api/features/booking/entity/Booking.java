package co.istad.rentiq_api.features.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_ref", nullable = false, unique = true)
    private String bookingRef;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "offer_id")
    private UUID offerId;

    @Column(name = "rental_start", nullable = false)
    private LocalDate rentalStart;

    @Column(name = "rental_end", nullable = false)
    private LocalDate rentalEnd;

    @Column(name = "rental_days", nullable = false)
    private Short rentalDays;

    @Column(name = "booked_price_per_day", nullable = false)
    private BigDecimal bookedPricePerDay;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(name = "security_deposit")
    private BigDecimal securityDeposit;

    @Column(name = "commission_rate")
    private BigDecimal commissionRate;

    @Column(name = "commission_amount")
    private BigDecimal commissionAmount;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Builder.Default
    @Column(nullable = false)
    private String currency = "USD";

    @Builder.Default
    @Column(nullable = false)
    private String status = "PENDING";

    @Builder.Default
    @Column(name = "payment_status", nullable = false)
    private String paymentStatus = "UNPAID";

    @Column(name = "owner_confirmed_at")
    private Instant ownerConfirmedAt;

    @Column(name = "security_deposit_returned_at")
    private Instant securityDepositReturnedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}