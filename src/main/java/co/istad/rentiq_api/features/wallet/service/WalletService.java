package co.istad.rentiq_api.features.wallet.service;

import co.istad.rentiq_api.features.wallet.dto.request.AdminWalletAdjustRequest;
import co.istad.rentiq_api.features.wallet.dto.request.AdminWalletTopupRequest;
import co.istad.rentiq_api.features.wallet.dto.response.AdminWalletTopupResponse;
import co.istad.rentiq_api.features.wallet.dto.response.WalletResponse;
import co.istad.rentiq_api.features.wallet.dto.response.WalletTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The direct Admin top-up ({@link #adminTopupWallet}) is the ONLY wallet funding path. The
 * legacy vendor-initiated top-up-request/webhook/admin-confirm flow was removed (backend audit
 * SEC-001/BUS-001) — it duplicated this path and its public webhook was forgeable.
 */
public interface WalletService {

    void grantWelcomeBonusIfEligible(String userId);

    WalletResponse getWallet(String ownerId);

    Page<WalletTransactionResponse> getTransactions(String ownerId, Pageable pageable);

    WalletTransactionResponse getTransaction(String ownerId, UUID transactionId);

    Page<WalletResponse> adminListWallets(Pageable pageable);

    WalletResponse adminGetWallet(UUID walletId);

    Page<WalletTransactionResponse> adminGetWalletTransactions(UUID walletId, Pageable pageable);

    WalletResponse adminAdjustWallet(UUID walletId, AdminWalletAdjustRequest request, String adminId);

    /**
     * Admin directly credits a vendor's platform-balance wallet after verifying an external
     * payment (cash / ABA / KHQR / bank transfer) the vendor made to the admin outside Rentiq.
     * Rentiq never collects or holds rental payment — this funds platform services only
     * (advertisements, promotions, commission/service fees), not rental earnings.
     */
    AdminWalletTopupResponse adminTopupWallet(UUID walletId, AdminWalletTopupRequest request, String adminId);

    /**
     * Debits a vendor's wallet for a Promotion purchase. Reuses the same locked
     * balance-mutation/negative-balance-protection choke point as every other wallet
     * operation. {@code currency} must match the wallet's own currency exactly — no
     * conversion is performed; a mismatch fails the charge (and, by extension, the whole
     * Promotion purchase transaction, since this participates in the caller's transaction).
     */
    void chargePromotion(UUID promotionId, String ownerId, BigDecimal amount, String currency);

    /**
     * Debits a vendor's wallet for an Advertisement purchase, charged only at successful admin
     * approval (never at submission, never on rejection). Mirrors {@link #chargePromotion}
     * exactly: same locked balance-mutation/negative-balance-protection choke point, same
     * no-conversion currency-mismatch failure. This participates in the caller's transaction
     * (the admin-approval transaction), so a failure here rolls back the whole approval.
     */
    void chargeAdvertisement(UUID advertisementId, String ownerId, BigDecimal amount, String currency);
}
