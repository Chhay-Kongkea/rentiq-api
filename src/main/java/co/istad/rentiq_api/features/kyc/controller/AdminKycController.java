package co.istad.rentiq_api.features.kyc.controller;

import co.istad.rentiq_api.features.kyc.dto.request.RejectKycRequest;
import co.istad.rentiq_api.features.kyc.dto.response.AdminKycDetailResponse;
import co.istad.rentiq_api.features.kyc.dto.response.AdminKycListItemResponse;
import co.istad.rentiq_api.features.kyc.service.KycService;
import co.istad.rentiq_api.security.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/kyc")
@RequiredArgsConstructor
public class AdminKycController {

    private final KycService kycService;

    @GetMapping
    public Page<AdminKycListItemResponse> listKyc(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return kycService.adminListKyc(status, pageable);
    }

    @GetMapping("/{kycId}")
    public AdminKycDetailResponse getKyc(@PathVariable UUID kycId) {
        return kycService.adminGetKyc(kycId);
    }

    @PatchMapping("/{kycId}/approve")
    public AdminKycDetailResponse approveKyc(@PathVariable UUID kycId) {
        return kycService.adminApproveKyc(kycId, AuthUtils.extractUserId());
    }

    @PatchMapping("/{kycId}/reject")
    public AdminKycDetailResponse rejectKyc(@PathVariable UUID kycId,
                                            @Valid @RequestBody RejectKycRequest request) {
        return kycService.adminRejectKyc(kycId, AuthUtils.extractUserId(), request);
    }
}