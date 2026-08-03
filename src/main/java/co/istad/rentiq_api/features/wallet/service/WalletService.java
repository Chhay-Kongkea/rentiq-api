package co.istad.rentiq_api.features.wallet.service;

import co.istad.rentiq_api.features.wallet.dto.request.AdminWalletAdjustRequest;
import co.istad.rentiq_api.features.wallet.dto.request.CreateTopupRequest;
import co.istad.rentiq_api.features.wallet.dto.request.TopupWebhookRequest;
import co.istad.rentiq_api.features.wallet.dto.response.TopupRequestResponse;
import co.istad.rentiq_api.features.wallet.dto.response.WalletResponse;
import co.istad.rentiq_api.features.wallet.dto.response.WalletTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface WalletService {

    void grantWelcomeBonusIfEligible(String userId);

    WalletResponse getWallet(String ownerId);

    Page<WalletTransactionResponse> getTransactions(String ownerId, Pageable pageable);

    WalletTransactionResponse getTransaction(String ownerId, UUID transactionId);

    TopupRequestResponse createTopupRequest(String ownerId, CreateTopupRequest request);

    Page<TopupRequestResponse> getTopupRequests(String ownerId, Pageable pageable);

    TopupRequestResponse getTopupRequest(String ownerId, UUID topupRequestId);

    TopupRequestResponse processTopupWebhook(TopupWebhookRequest request, String signature);

    Page<WalletResponse> adminListWallets(Pageable pageable);

    WalletResponse adminGetWallet(UUID walletId);

    Page<WalletTransactionResponse> adminGetWalletTransactions(UUID walletId, Pageable pageable);

    WalletResponse adminAdjustWallet(UUID walletId, AdminWalletAdjustRequest request, String adminId);

    Page<TopupRequestResponse> adminListTopupRequests(Pageable pageable);

    TopupRequestResponse adminConfirmTopup(UUID topupRequestId, String adminId);
}
