package co.istad.rentiq_api.features.wallet.repository;


import co.istad.rentiq_api.features.wallet.entity.TopupRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;
import co.istad.rentiq_api.features.wallet.enums.TopupStatus;

public interface TopupRequestRepository extends JpaRepository<TopupRequest, UUID> {

    Page<TopupRequest> findByWalletId(UUID walletId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TopupRequest t where t.bankReference = :bankReference")
    Optional<TopupRequest> findByBankReferenceForUpdate(@Param("bankReference") String bankReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TopupRequest t where t.id = :id")
    Optional<TopupRequest> findByIdForUpdate(@Param("id") UUID id);

    long countByStatus(TopupStatus status);

    Page<TopupRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
