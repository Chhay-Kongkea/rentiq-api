package co.istad.rentiq_api.features.advertisement.repository;

import co.istad.rentiq_api.features.advertisement.entity.Advertisement;
import co.istad.rentiq_api.features.advertisement.enums.AdvertisementStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdvertisementRepository
        extends JpaRepository<Advertisement, UUID>, JpaSpecificationExecutor<Advertisement> {

    Page<Advertisement> findByVendorId(String vendorId, Pageable pageable);

    Page<Advertisement> findByVendorIdAndStatus(String vendorId, AdvertisementStatus status, Pageable pageable);

    /**
     * Locks the advertisement row (SELECT ... FOR UPDATE) so a concurrent or repeated admin
     * moderation call for the same ad serializes instead of double-processing it.
     * Must only be called inside an existing @Transactional method.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Advertisement a where a.id = :id")
    Optional<Advertisement> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Publicly visible ads: an explicit statuses+time-window check, not just the stored status —
     * this is the source of truth for "is this ad live right now", independent of whether any
     * scheduled job has flipped APPROVED->ACTIVE or ACTIVE->EXPIRED yet. Also excludes ads whose
     * item has since been deleted.
     */
    @Query("""
            select a from Advertisement a
            where a.status in :statuses
              and a.startAt <= :now and a.endAt > :now
              and (:itemId is null or a.itemId = :itemId)
              and exists (select 1 from Item i where i.id = a.itemId and i.deleted = false)
            order by a.startAt desc
            """)
    Page<Advertisement> findPubliclyVisible(
            @Param("statuses") List<AdvertisementStatus> statuses,
            @Param("now") OffsetDateTime now,
            @Param("itemId") UUID itemId,
            Pageable pageable);

    @Query("""
            select a from Advertisement a
            where a.id = :id
              and a.status in :statuses
              and a.startAt <= :now and a.endAt > :now
              and exists (select 1 from Item i where i.id = a.itemId and i.deleted = false)
            """)
    Optional<Advertisement> findPubliclyVisibleById(
            @Param("id") UUID id,
            @Param("statuses") List<AdvertisementStatus> statuses,
            @Param("now") OffsetDateTime now);
}
