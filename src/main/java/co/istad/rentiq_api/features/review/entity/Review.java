package co.istad.rentiq_api.features.review.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_id", nullable = false, unique = true)
    private UUID bookingId;

    @Column(name = "reviewer_id", nullable = false)
    private String reviewerId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(nullable = false)
    private Short rating;

    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;

    @Column(name = "vendor_reply", columnDefinition = "TEXT")
    private String vendorReply;

    @Column(name = "vendor_replied_at")
    private Instant vendorRepliedAt;

    @Builder.Default
    @Column(nullable = false)
    private String status = "VISIBLE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}