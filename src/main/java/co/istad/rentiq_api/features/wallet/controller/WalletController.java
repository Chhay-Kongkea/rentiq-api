package co.istad.rentiq_api.features.wallet.controller;

import co.istad.rentiq_api.features.wallet.dto.request.CreateTopupRequest;
import co.istad.rentiq_api.features.wallet.dto.request.TopupWebhookRequest;
import co.istad.rentiq_api.features.wallet.dto.response.TopupRequestResponse;
import co.istad.rentiq_api.features.wallet.dto.response.WalletResponse;
import co.istad.rentiq_api.features.wallet.dto.response.WalletTransactionResponse;
import co.istad.rentiq_api.features.wallet.service.WalletService;
import co.istad.rentiq_api.security.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('VENDOR')")
    public WalletResponse getMyWallet() {
        return walletService.getWallet(AuthUtils.extractUserId());
    }

    @GetMapping("/me/transactions")
    @PreAuthorize("hasRole('VENDOR')")
    public Page<WalletTransactionResponse> getMyTransactions(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return walletService.getTransactions(AuthUtils.extractUserId(), pageable);
    }

    @GetMapping("/me/transactions/{transactionId}")
    @PreAuthorize("hasRole('VENDOR')")
    public WalletTransactionResponse getMyTransaction(@PathVariable UUID transactionId) {
        return walletService.getTransaction(AuthUtils.extractUserId(), transactionId);
    }

    @PostMapping("/me/topup-requests")
    @PreAuthorize("hasRole('VENDOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public TopupRequestResponse createTopupRequest(@Valid @RequestBody CreateTopupRequest request) {
        return walletService.createTopupRequest(AuthUtils.extractUserId(), request);
    }

    @GetMapping("/me/topup-requests")
    @PreAuthorize("hasRole('VENDOR')")
    public Page<TopupRequestResponse> getMyTopupRequests(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return walletService.getTopupRequests(AuthUtils.extractUserId(), pageable);
    }

    @GetMapping("/me/topup-requests/{topupRequestId}")
    @PreAuthorize("hasRole('VENDOR')")
    public TopupRequestResponse getMyTopupRequest(@PathVariable UUID topupRequestId) {
        return walletService.getTopupRequest(AuthUtils.extractUserId(), topupRequestId);
    }

    @PostMapping("/topup-requests/webhook")
    public TopupRequestResponse handleTopupWebhook(
            @Valid @RequestBody TopupWebhookRequest request,
            @RequestHeader("X-Webhook-Signature") String signature
    ) {
        return walletService.processTopupWebhook(request, signature);
    }
}
