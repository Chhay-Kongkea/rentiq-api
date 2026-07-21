package co.istad.rentiq_api.features.bookingInspection.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inspection_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "inspection_id", nullable = false)
    private UUID inspectionId;

    @Column(name = "image_name", nullable = false)
    private String imageName;

    @Column(nullable = false)
    private String type; // "CHECK_IN" or "CHECK_OUT"

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}