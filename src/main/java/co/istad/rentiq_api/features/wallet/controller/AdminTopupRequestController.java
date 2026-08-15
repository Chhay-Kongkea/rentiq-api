package co.istad.rentiq_api.features.wallet.controller;

import co.istad.rentiq_api.features.wallet.dto.response.TopupRequestResponse;
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

@RestController
@RequestMapping("/api/v1/admin/topup-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTopupRequestController {

    private final WalletService walletService;

    @GetMapping
    public Page<TopupRequestResponse> listTopupRequests(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return walletService.adminListTopupRequests(pageable);
    }

    @PatchMapping("/{topupRequestId}/confirm")
    public TopupRequestResponse confirmTopup(@PathVariable UUID topupRequestId) {
        return walletService.adminConfirmTopup(topupRequestId, AuthUtils.extractUserId());
    }
}
