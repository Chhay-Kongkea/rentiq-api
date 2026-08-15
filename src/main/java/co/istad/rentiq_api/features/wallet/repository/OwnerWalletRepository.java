package co.istad.rentiq_api.features.wallet.repository;


import co.istad.rentiq_api.features.wallet.entity.OwnerWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OwnerWalletRepository extends JpaRepository<OwnerWallet, UUID> {

    Optional<OwnerWallet> findByOwnerId(String ownerId);

    /**
     * Locks the wallet row (SELECT ... FOR UPDATE) so concurrent balance mutations serialize.
     * Must only be called inside an existing @Transactional method.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from OwnerWallet w where w.id = :id")
    Optional<OwnerWallet> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from OwnerWallet w where w.ownerId = :ownerId")
    Optional<OwnerWallet> findByOwnerIdForUpdate(@Param("ownerId") String ownerId);
}
