package co.istad.rentiq_api.features.platformSetting.repository;

import co.istad.rentiq_api.features.platformSetting.entity.PlatformSetting;
import co.istad.rentiq_api.features.platformSetting.enums.PlatformSettingKey;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, PlatformSettingKey> {

    /**
     * Locks the override row (if one exists) so concurrent Admin updates to the same key
     * serialize instead of racing. Must only be called inside an existing @Transactional method.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PlatformSetting s where s.key = :key")
    Optional<PlatformSetting> findByKeyForUpdate(@Param("key") PlatformSettingKey key);
}
