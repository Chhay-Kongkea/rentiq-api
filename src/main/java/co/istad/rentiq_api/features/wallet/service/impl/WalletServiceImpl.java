package co.istad.rentiq_api.features.wallet.service.impl;


import co.istad.rentiq_api.common.config.props.WalletGatewayProperties;
import co.istad.rentiq_api.features.wallet.dto.request.AdminWalletAdjustRequest;
import co.istad.rentiq_api.features.wallet.dto.request.CreateTopupRequest;
import co.istad.rentiq_api.features.wallet.dto.request.TopupWebhookRequest;
import co.istad.rentiq_api.features.wallet.dto.response.TopupRequestResponse;
import co.istad.rentiq_api.features.wallet.dto.response.WalletResponse;
import co.istad.rentiq_api.features.wallet.dto.response.WalletTransactionResponse;
import co.istad.rentiq_api.features.wallet.entity.OwnerWallet;
import co.istad.rentiq_api.features.wallet.entity.TopupRequest;
import co.istad.rentiq_api.features.wallet.entity.WalletTransaction;
import co.istad.rentiq_api.features.wallet.enums.TopupStatus;
import co.istad.rentiq_api.features.wallet.enums.TransactionDirection;
import co.istad.rentiq_api.features.wallet.enums.TransactionType;
import co.istad.rentiq_api.features.wallet.enums.WalletStatus;
import co.istad.rentiq_api.features.wallet.exception.WalletException;
import co.istad.rentiq_api.features.wallet.mapper.WalletMapper;
import co.istad.rentiq_api.features.wallet.repository.OwnerWalletRepository;
import co.istad.rentiq_api.features.wallet.repository.TopupRequestRepository;
import co.istad.rentiq_api.features.wallet.repository.WalletTransactionRepository;
import co.istad.rentiq_api.features.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private static final BigDecimal WELCOME_BONUS_AMOUNT = new BigDecimal("5.00");
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final OwnerWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final TopupRequestRepository topupRequestRepository;
    private final WalletMapper walletMapper;
    private final WalletGatewayProperties gatewayProperties;

    @Override
    @Transactional
    public void grantWelcomeBonusIfEligible(String userId) {

        OwnerWallet wallet = walletRepository.findByOwnerIdForUpdate(userId)
                .orElseGet(() -> walletRepository.save(
                        OwnerWallet.builder()
                                .ownerId(userId)
                                .balance(BigDecimal.ZERO)
                                .build()
                ));


        if (transactionRepository.existsByWalletIdAndTransactionType(wallet.getId(), TransactionType.WELCOME_BONUS)) {
            log.info("Welcome bonus already granted for user {}, skipping", userId);
            return;
        }

        applyBalanceChange(
                wallet,
                WELCOME_BONUS_AMOUNT,
                TransactionDirection.IN,
                TransactionType.WELCOME_BONUS,
                null, null, null, null,
                "Welcome bonus for KYC approval"
        );

        log.info("Welcome bonus of {} granted to user {}", WELCOME_BONUS_AMOUNT, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWallet(String ownerId) {
        return walletMapper.toResponse(requireWallet(ownerId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getTransactions(String ownerId, Pageable pageable) {
        OwnerWallet wallet = requireWallet(ownerId);
        return transactionRepository.findByWalletId(wallet.getId(), pageable).map(walletMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletTransactionResponse getTransaction(String ownerId, UUID transactionId) {
        OwnerWallet wallet = requireWallet(ownerId);

        WalletTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> WalletException.transactionNotFound(transactionId));

        if (!transaction.getWalletId().equals(wallet.getId())) {
            throw WalletException.transactionNotFound(transactionId);
        }

        return walletMapper.toResponse(transaction);
    }

    @Override
    @Transactional
    public TopupRequestResponse createTopupRequest(String ownerId, CreateTopupRequest request) {
        OwnerWallet wallet = walletRepository.findByOwnerId(ownerId)
                .orElseGet(() -> walletRepository.save(
                        OwnerWallet.builder()
                                .ownerId(ownerId)
                                .balance(BigDecimal.ZERO)
                                .build()
                ));

        TopupRequest topup = TopupRequest.builder()
                .walletId(wallet.getId())
                .amount(request.amount())
                .paymentMethod(request.paymentMethod() == null || request.paymentMethod().isBlank()
                        ? "KHQR" : request.paymentMethod())
                .status(TopupStatus.PENDING)
                .bankReference(UUID.randomUUID().toString())
                .build();

        return walletMapper.toResponse(topupRequestRepository.save(topup));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TopupRequestResponse> getTopupRequests(String ownerId, Pageable pageable) {
        OwnerWallet wallet = requireWallet(ownerId);
        return topupRequestRepository.findByWalletId(wallet.getId(), pageable).map(walletMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TopupRequestResponse getTopupRequest(String ownerId, UUID topupRequestId) {
        OwnerWallet wallet = requireWallet(ownerId);

        TopupRequest topup = topupRequestRepository.findById(topupRequestId)
                .orElseThrow(() -> WalletException.topupNotFound(topupRequestId));

        if (!topup.getWalletId().equals(wallet.getId())) {
            throw WalletException.topupNotFound(topupRequestId);
        }

        return walletMapper.toResponse(topup);
    }

    @Override
    @Transactional
    public TopupRequestResponse processTopupWebhook(TopupWebhookRequest request, String signature) {
        verifySignature(request, signature);

        // Row-level lock on the bank reference — a redelivered webhook for the same
        // reference blocks here instead of racing a concurrent credit.
        TopupRequest topup = topupRequestRepository.findByBankReferenceForUpdate(request.bankReference())
                .orElseThrow(() -> WalletException.topupNotFoundByReference(request.bankReference()));

        if (topup.getStatus() != TopupStatus.PENDING) {
            // Already processed — idempotent no-op so gateway retries are safe.
            log.info("Ignoring replayed webhook for bank reference {} (status already {})",
                    request.bankReference(), topup.getStatus());
            return walletMapper.toResponse(topup);
        }

        if (request.amount().compareTo(topup.getAmount()) != 0) {
            throw WalletException.amountMismatch();
        }

        if (request.status() == TopupStatus.SUCCESS) {
            OwnerWallet wallet = walletRepository.findByIdForUpdate(topup.getWalletId())
                    .orElseThrow(() -> WalletException.notFoundById(topup.getWalletId()));

            applyBalanceChange(
                    wallet,
                    topup.getAmount(),
                    TransactionDirection.IN,
                    TransactionType.TOP_UP,
                    null, topup.getId(), null, null,
                    "Wallet top-up via " + topup.getPaymentMethod()
            );

            topup.setStatus(TopupStatus.SUCCESS);
        } else {
            topup.setStatus(TopupStatus.EXPIRED);
        }

        topupRequestRepository.save(topup);
        return walletMapper.toResponse(topup);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletResponse> adminListWallets(Pageable pageable) {
        return walletRepository.findAll(pageable).map(walletMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse adminGetWallet(UUID walletId) {
        return walletMapper.toResponse(
                walletRepository.findById(walletId).orElseThrow(() -> WalletException.notFoundById(walletId))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> adminGetWalletTransactions(UUID walletId, Pageable pageable) {
        if (!walletRepository.existsById(walletId)) {
            throw WalletException.notFoundById(walletId);
        }
        return transactionRepository.findByWalletId(walletId, pageable).map(walletMapper::toResponse);
    }

    @Override
    @Transactional
    public WalletResponse adminAdjustWallet(UUID walletId, AdminWalletAdjustRequest request, String adminId) {
        OwnerWallet wallet = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> WalletException.notFoundById(walletId));

        applyBalanceChange(
                wallet,
                request.amount(),
                request.direction(),
                TransactionType.ADMIN_ADJUSTMENT,
                null, null, null, null,
                "Manual adjustment by admin %s: %s".formatted(adminId, request.reason())
        );

        return walletMapper.toResponse(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TopupRequestResponse> adminListTopupRequests(Pageable pageable) {
        return topupRequestRepository.findAll(pageable).map(walletMapper::toResponse);
    }

    @Override
    @Transactional
    public TopupRequestResponse adminConfirmTopup(UUID topupRequestId, String adminId) {
        TopupRequest topup = topupRequestRepository.findByIdForUpdate(topupRequestId)
                .orElseThrow(() -> WalletException.topupNotFound(topupRequestId));

        if (topup.getStatus() != TopupStatus.PENDING) {
            throw WalletException.invalidTopupState(topup.getStatus().name());
        }

        OwnerWallet wallet = walletRepository.findByIdForUpdate(topup.getWalletId())
                .orElseThrow(() -> WalletException.notFoundById(topup.getWalletId()));

        // Amount always comes from the stored request, never from this call's input.
        applyBalanceChange(
                wallet,
                topup.getAmount(),
                TransactionDirection.IN,
                TransactionType.TOP_UP,
                null, topup.getId(), null, null,
                "Manual top-up confirmation by admin " + adminId
        );

        topup.setStatus(TopupStatus.SUCCESS);
        topupRequestRepository.save(topup);

        return walletMapper.toResponse(topup);
    }

    private OwnerWallet requireWallet(String ownerId) {
        return walletRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> WalletException.notFoundForOwner(ownerId));
    }

    /**
     * Single choke point for every balance mutation: applies the delta to the locked
     * wallet row and writes the matching ledger row in the same transaction as the caller.
     */
    private WalletTransaction applyBalanceChange(
            OwnerWallet wallet,
            BigDecimal amount,
            TransactionDirection direction,
            TransactionType type,
            UUID bookingId,
            UUID topupRequestId,
            UUID advertisementId,
            UUID promotionId,
            String description
    ) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw WalletException.invalidAmount();
        }

        BigDecimal newBalance = direction == TransactionDirection.IN
                ? wallet.getBalance().add(amount)
                : wallet.getBalance().subtract(amount);

        wallet.setBalance(newBalance);
        wallet.setStatus(newBalance.compareTo(BigDecimal.ZERO) <= 0 ? WalletStatus.LOCKED : WalletStatus.ACTIVE);
        walletRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.builder()
                .walletId(wallet.getId())
                .transactionType(type)
                .amount(amount)
                .direction(direction)
                .balanceAfter(newBalance)
                .bookingId(bookingId)
                .topupRequestId(topupRequestId)
                .advertisementId(advertisementId)
                .promotionId(promotionId)
                .description(description)
                .build();

        return transactionRepository.save(transaction);
    }

    private void verifySignature(TopupWebhookRequest request, String signature) {
        if (signature == null || signature.isBlank()) {
            throw WalletException.invalidSignature();
        }

        String payload = request.bankReference() + ":" + request.amount().toPlainString() + ":" + request.status();
        String expected = hmacSha256Hex(payload, gatewayProperties.getWebhookSecret());

        boolean valid = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8)
        );

        if (!valid) {
            throw WalletException.invalidSignature();
        }
    }

    private static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));

            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);

            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to compute webhook HMAC signature", e);
        }
    }
}
