package co.istad.rentiq_api.features.kyc.entity;


import co.istad.rentiq_api.features.kyc.KycStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_kyc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserKyc {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", unique = true, nullable = false, length = 255)
    private String userId;

    @Column(name = "national_id_number")
    private String nationalIdNumber;

    @Column(name = "national_id_type", length = 50)
    private String nationalIdType;

    @Column(name = "national_id_country", length = 3)
    @Builder.Default
    private String nationalIdCountry = "KHM";

    @Column(name = "front_image_url", length = 500)
    private String frontImageUrl;

    @Column(name = "back_image_url", length = 500)
    private String backImageUrl;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;



    @Column(name = "is_email_verified")
    @Builder.Default
    private boolean emailVerified = false;


    @Column(name = "verification_status", length = 30)
    @Builder.Default
    private String verificationStatus = KycStatus.PENDING.name();

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "reviewed_by", length = 255)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}