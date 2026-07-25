package co.istad.rentiq_api.features.bookingInspection.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking_inspections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_id", nullable = false, unique = true)
    private UUID bookingId;

    @Column(name = "check_in_notes", columnDefinition = "TEXT")
    private String checkInNotes;

    @Column(name = "check_out_notes", columnDefinition = "TEXT")
    private String checkOutNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}