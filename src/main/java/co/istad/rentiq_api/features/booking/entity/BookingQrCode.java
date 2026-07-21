package co.istad.rentiq_api.features.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking_qr_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingQrCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_id", nullable = false, unique = true)
    private UUID bookingId;

    @Column(name = "qr_token", nullable = false, unique = true)
    private String qrToken;

    @Builder.Default
    @Column(name = "is_valid", nullable = false)
    private Boolean isValid = true;

    @Column(name = "scanned_at")
    private Instant scannedAt;

    @Column(name = "scanned_by")
    private String scannedBy;

    @Column(name = "expires_at")
    private Instant expiresAt;
}