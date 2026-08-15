package co.istad.rentiq_api.features.bookings.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "booking_qr_codes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingQrCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(name = "qr_token", nullable = false, unique = true, length = 255)
    private String qrToken;

    @Column(name = "is_valid", nullable = false)
    @Builder.Default
    private Boolean isValid = true;

    @Column(name = "scanned_at")
    private OffsetDateTime scannedAt;

    @Column(name = "scanned_by", length = 255)
    private String scannedBy;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;
}
