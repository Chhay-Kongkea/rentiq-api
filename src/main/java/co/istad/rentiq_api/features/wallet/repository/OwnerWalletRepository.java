package co.istad.rentiq_api.features.wallet.repository;



import co.istad.rentiq_api.features.wallet.entity.OwnerWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OwnerWalletRepository extends JpaRepository<OwnerWallet, java.util.UUID> {
    Optional<OwnerWallet> findByOwnerId(String ownerId);
}