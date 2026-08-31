package co.istad.rentiq_api.features.wallet.controller;

import co.istad.rentiq_api.features.wallet.dto.request.AdminWalletAdjustRequest;
import co.istad.rentiq_api.features.wallet.dto.request.AdminWalletTopupRequest;
import co.istad.rentiq_api.features.wallet.dto.response.AdminWalletTopupResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/wallets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminWalletController {

    private final WalletService walletService;

    @GetMapping
    public Page<WalletResponse> listWallets(@PageableDefault(size = 20) Pageable pageable) {
        return walletService.adminListWallets(pageable);
    }

    @GetMapping("/{walletId}")
    public WalletResponse getWallet(@PathVariable UUID walletId) {
        return walletService.adminGetWallet(walletId);
    }

    @GetMapping("/{walletId}/transactions")
    public Page<WalletTransactionResponse> getWalletTransactions(
            @PathVariable UUID walletId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return walletService.adminGetWalletTransactions(walletId, pageable);
    }

    @PatchMapping("/{walletId}/adjust")
    public WalletResponse adjustWallet(
            @PathVariable UUID walletId,
            @Valid @RequestBody AdminWalletAdjustRequest request
    ) {
        return walletService.adminAdjustWallet(walletId, request, AuthUtils.extractUserId());
    }

    /**
     * Vendor paid the admin externally (cash / ABA / KHQR / bank transfer); the admin verifies
     * that payment and credits the vendor's Rentiq platform-balance wallet here. Distinct from
     * {@link #adjustWallet} (a manual balance correction, not tied to a real vendor payment).
     */
    @PostMapping("/{walletId}/topup")
    public AdminWalletTopupResponse topupWallet(
            @PathVariable UUID walletId,
            @Valid @RequestBody AdminWalletTopupRequest request
    ) {
        return walletService.adminTopupWallet(walletId, request, AuthUtils.extractUserId());
    }
}
