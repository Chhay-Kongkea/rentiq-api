package co.istad.rentiq_api.features.wallet.service.impl;


import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
import co.istad.rentiq_api.features.notification.enums.NotificationReferenceType;
import co.istad.rentiq_api.features.notification.enums.NotificationType;
import co.istad.rentiq_api.features.notification.service.NotificationService;
import co.istad.rentiq_api.features.vendorApplication.enums.VendorApplicationStatus;
import co.istad.rentiq_api.features.vendorApplication.repository.VendorApplicationRepository;
import co.istad.rentiq_api.features.wallet.dto.request.AdminWalletAdjustRequest;
import co.istad.rentiq_api.features.wallet.dto.request.AdminWalletTopupRequest;
import co.istad.rentiq_api.features.wallet.dto.response.AdminWalletTopupResponse;
import co.istad.rentiq_api.features.wallet.dto.response.WalletResponse;
import co.istad.rentiq_api.features.wallet.dto.response.WalletTransactionResponse;
import co.istad.rentiq_api.features.wallet.entity.OwnerWallet;
import co.istad.rentiq_api.features.wallet.entity.WalletTransaction;
import co.istad.rentiq_api.features.wallet.enums.TransactionDirection;
import co.istad.rentiq_api.features.wallet.enums.TransactionType;
import co.istad.rentiq_api.features.wallet.enums.WalletStatus;
import co.istad.rentiq_api.features.wallet.exception.WalletException;
import co.istad.rentiq_api.features.wallet.mapper.WalletMapper;
import co.istad.rentiq_api.features.wallet.repository.OwnerWalletRepository;
import co.istad.rentiq_api.features.wallet.repository.WalletTransactionRepository;
import co.istad.rentiq_api.features.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * The ONLY way to credit a wallet with TOP_UP/IN is {@link #adminTopupWallet} — Admin verifies
 * an external payment and directly credits the vendor's wallet. The legacy vendor-initiated
 * top-up-request + public webhook + admin-confirm flow (SEC-001 / BUS-001 in the backend audit)
 * has been removed: it duplicated this funding path and its webhook was reachable with a
 * committed fallback signing secret, allowing forged wallet credits. See
 * {@code TopupRequest}/{@code TopupRequestRepository} for why those types remain (read-only,
 * Admin Dashboard "recent activity" feed only — no code path writes to them anymore).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private static final BigDecimal WELCOME_BONUS_AMOUNT = new BigDecimal("5.00");

    private final OwnerWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final WalletMapper walletMapper;
    private final AdminAuditService adminAuditService;
    private final VendorApplicationRepository vendorApplicationRepository;
    private final NotificationService notificationService;

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

        BigDecimal previousBalance = wallet.getBalance();

        applyBalanceChange(
                wallet,
                request.amount(),
                request.direction(),
                TransactionType.ADMIN_ADJUSTMENT,
                null, null, null, null,
                "Manual adjustment by admin %s: %s".formatted(adminId, request.reason())
        );

        adminAuditService.record(
                request.direction() == TransactionDirection.IN
                        ? AdminAuditAction.WALLET_CREDITED : AdminAuditAction.WALLET_DEBITED,
                AdminAuditTargetType.WALLET,
                walletId.toString(),
                Map.of("balance", previousBalance),
                Map.of("balance", wallet.getBalance(), "adjustmentAmount", request.amount()),
                request.reason());

        return walletMapper.toResponse(wallet);
    }

    @Override
    @Transactional
    public AdminWalletTopupResponse adminTopupWallet(UUID walletId, AdminWalletTopupRequest request, String adminId) {
        OwnerWallet wallet = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> WalletException.notFoundById(walletId));

        boolean isApprovedVendor = vendorApplicationRepository.findByUserId(wallet.getOwnerId())
                .map(application -> application.getStatus() == VendorApplicationStatus.APPROVED)
                .orElse(false);
        if (!isApprovedVendor) {
            throw WalletException.notAVendor(wallet.getOwnerId());
        }

        if (!request.currency().equals(wallet.getCurrency())) {
            throw WalletException.currencyMismatch(wallet.getCurrency(), request.currency());
        }

        BigDecimal balanceBefore = wallet.getBalance();

        WalletTransaction transaction = applyBalanceChange(
                wallet,
                request.amount(),
                TransactionDirection.IN,
                TransactionType.TOP_UP,
                null, null, null, null,
                topupDescription(adminId, request)
        );

        adminAuditService.record(
                AdminAuditAction.WALLET_TOPPED_UP,
                AdminAuditTargetType.WALLET,
                walletId.toString(),
                Map.of("balance", balanceBefore),
                Map.of("balance", wallet.getBalance(), "topUpAmount", request.amount(), "currency", request.currency()),
                request.note());

        notificationService.notifyUser(
                wallet.getOwnerId(),
                NotificationType.PAYMENT,
                "Wallet credited",
                "Your Rentiq wallet was credited with " + request.amount() + " " + request.currency() + ".",
                NotificationReferenceType.PAYMENT,
                transaction.getId());

        return new AdminWalletTopupResponse(
                walletId, wallet.getOwnerId(), request.amount(), request.currency(),
                balanceBefore, wallet.getBalance(), transaction.getId(), transaction.getCreatedAt());
    }

    private static String topupDescription(String adminId, AdminWalletTopupRequest request) {
        StringBuilder description = new StringBuilder("Vendor paid admin ").append(adminId).append(" externally");
        if (request.paymentMethod() != null && !request.paymentMethod().isBlank()) {
            description.append(" via ").append(request.paymentMethod());
        }
        if (request.paymentReference() != null && !request.paymentReference().isBlank()) {
            description.append(" (ref: ").append(request.paymentReference()).append(')');
        }
        if (request.note() != null && !request.note().isBlank()) {
            description.append(" — ").append(request.note());
        }
        return description.toString();
    }

    @Override
    @Transactional
    public void chargePromotion(UUID promotionId, String ownerId, BigDecimal amount, String currency) {
        OwnerWallet wallet = walletRepository.findByOwnerIdForUpdate(ownerId)
                .orElseThrow(() -> WalletException.notFoundForOwner(ownerId));

        if (!wallet.getCurrency().equals(currency)) {
            throw WalletException.currencyMismatch(wallet.getCurrency(), currency);
        }

        applyBalanceChange(
                wallet,
                amount,
                TransactionDirection.OUT,
                TransactionType.PROMOTION,
                null, null, null, promotionId,
                "Promotion charge for promotion " + promotionId
        );
    }

    @Override
    @Transactional
    public void chargeAdvertisement(UUID advertisementId, String ownerId, BigDecimal amount, String currency) {
        OwnerWallet wallet = walletRepository.findByOwnerIdForUpdate(ownerId)
                .orElseThrow(() -> WalletException.notFoundForOwner(ownerId));

        if (!wallet.getCurrency().equals(currency)) {
            throw WalletException.currencyMismatch(wallet.getCurrency(), currency);
        }

        applyBalanceChange(
                wallet,
                amount,
                TransactionDirection.OUT,
                TransactionType.ADVERTISEMENT,
                null, null, advertisementId, null,
                "Advertisement charge for advertisement " + advertisementId
        );
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

        if (direction == TransactionDirection.OUT && wallet.getBalance().compareTo(amount) < 0) {
            throw WalletException.insufficientBalance(wallet.getBalance(), amount);
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
}
