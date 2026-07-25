package co.istad.rentiq_api.features.wallet.repository;


import co.istad.rentiq_api.features.wallet.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {
    boolean existsByWalletIdAndTransactionType(UUID walletId, String transactionType);
}