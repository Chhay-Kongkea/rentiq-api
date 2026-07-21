package co.istad.rentiq_api.features.wallet.service.impl;



import co.istad.rentiq_api.features.wallet.entity.OwnerWallet;
import co.istad.rentiq_api.features.wallet.entity.WalletTransaction;
import co.istad.rentiq_api.features.wallet.repository.OwnerWalletRepository;
import co.istad.rentiq_api.features.wallet.repository.WalletTransactionRepository;
import co.istad.rentiq_api.features.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private static final BigDecimal WELCOME_BONUS_AMOUNT = new BigDecimal("5.00");
    private static final String WELCOME_BONUS_TYPE = "WELCOME_BONUS";

    private final OwnerWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    @Override
    @Transactional
    public void grantWelcomeBonusIfEligible(String userId) {

        OwnerWallet wallet = walletRepository.findByOwnerId(userId)
                .orElseGet(() -> walletRepository.save(
                        OwnerWallet.builder()
                                .ownerId(userId)
                                .balance(BigDecimal.ZERO)
                                .build()
                ));

        // Idempotency guard — never grant the welcome bonus twice for the same wallet.
        if (transactionRepository.existsByWalletIdAndTransactionType(wallet.getId(), WELCOME_BONUS_TYPE)) {
            log.info("Welcome bonus already granted for user {}, skipping", userId);
            return;
        }

        BigDecimal newBalance = wallet.getBalance().add(WELCOME_BONUS_AMOUNT);
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.builder()
                .walletId(wallet.getId())
                .transactionType(WELCOME_BONUS_TYPE)
                .amount(WELCOME_BONUS_AMOUNT)
                .direction("IN")
                .balanceAfter(newBalance)
                .description("Welcome bonus for KYC approval")
                .build();

        transactionRepository.save(transaction);
        log.info("Welcome bonus of {} granted to user {}", WELCOME_BONUS_AMOUNT, userId);
    }
}