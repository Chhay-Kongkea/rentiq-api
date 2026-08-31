package co.istad.rentiq_api.features.promotion.repository;

import co.istad.rentiq_api.features.promotion.entity.Promotion;
import co.istad.rentiq_api.features.promotion.enums.PromotionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PromotionRepository
        extends JpaRepository<Promotion, UUID>, JpaSpecificationExecutor<Promotion> {

    Page<Promotion> findByVendorId(String vendorId, Pageable pageable);

    /**
     * Locks the promotion row so a concurrent or repeated cancel/suspend call for the same
     * promotion serializes instead of double-processing it.
     * Must only be called inside an existing @Transactional method.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Promotion p where p.id = :id")
    Optional<Promotion> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Purchase-time check for the one-effective-active-promotion-per-item rule. Race-free only
     * because the caller has already locked the Item row (see ItemRepository.findByIdForUpdate)
     * before calling this — every concurrent purchase attempt for the same item serializes on
     * that lock, so this read always reflects the latest committed state.
     */
    @Query("""
            select count(p) > 0 from Promotion p
            where p.itemId = :itemId
              and p.status = co.istad.rentiq_api.features.promotion.enums.PromotionStatus.ACTIVE
              and p.startAt <= :now and p.endAt > :now
            """)
    boolean existsEffectiveActiveForItem(@Param("itemId") UUID itemId, @Param("now") OffsetDateTime now);

    /**
     * Atomic increment — avoids the classic load/increment/save race where concurrent calls
     * lose updates. The WHERE clause itself enforces "only effectively active promotions
     * accumulate impressions", so a single statement both guards and increments.
     */
    @Modifying
    @Query("""
            update Promotion p set p.impressionCount = p.impressionCount + 1
            where p.id = :id
              and p.status = co.istad.rentiq_api.features.promotion.enums.PromotionStatus.ACTIVE
              and p.startAt <= :now and p.endAt > :now
            """)
    int incrementImpressionIfActive(@Param("id") UUID id, @Param("now") OffsetDateTime now);

    @Modifying
    @Query("""
            update Promotion p set p.clickCount = p.clickCount + 1
            where p.id = :id
              and p.status = co.istad.rentiq_api.features.promotion.enums.PromotionStatus.ACTIVE
              and p.startAt <= :now and p.endAt > :now
            """)
    int incrementClickIfActive(@Param("id") UUID id, @Param("now") OffsetDateTime now);
}
