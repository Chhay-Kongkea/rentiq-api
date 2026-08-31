package co.istad.rentiq_api.features.platformSetting.entity;

import co.istad.rentiq_api.features.platformSetting.enums.PlatformSettingKey;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Stores ONLY the Admin override for a setting — never a row for every predefined key. If no
 * row exists for a given {@link PlatformSettingKey}, the effective value is that key's
 * defaultValue (see PlatformSettingService). This is what avoids a seed migration: an empty
 * table is a valid, fully-functional state that reproduces today's hard-coded prices exactly.
 */
@Entity
@Table(name = "platform_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformSetting {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "setting_key", length = 60)
    private PlatformSettingKey key;

    @Column(name = "value", nullable = false, precision = 15, scale = 2)
    private BigDecimal value;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
