package co.istad.rentiq_api.features.userProfile.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(name = "id", length = 255)
    private String id; // Keycloak subject (sub) id

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "locale", length = 10)
    @Builder.Default
    private String locale = "en";

    @Column(name = "account_status", length = 20)
    @Builder.Default
    private String accountStatus = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}