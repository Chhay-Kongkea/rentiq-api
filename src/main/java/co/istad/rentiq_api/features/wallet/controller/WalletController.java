package co.istad.rentiq_api.features.wallet.controller;

import co.istad.rentiq_api.features.wallet.dto.response.WalletResponse;
import co.istad.rentiq_api.features.wallet.dto.response.WalletTransactionResponse;
import co.istad.rentiq_api.features.wallet.service.WalletService;
import co.istad.rentiq_api.security.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Vendor-facing read-only wallet view. There is no vendor-initiated top-up path here — funding
 * only happens via {@code AdminWalletController.topupWallet} (Admin verifies an external
 * payment and credits directly). See that controller's javadoc for why.
 */
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
}
