package co.istad.rentiq_api.features.promotion.controller;

import co.istad.rentiq_api.features.promotion.dto.request.SuspendPromotionRequest;
import co.istad.rentiq_api.features.promotion.dto.response.PromotionResponse;
import co.istad.rentiq_api.features.promotion.enums.PromotionPackage;
import co.istad.rentiq_api.features.promotion.enums.PromotionStatus;
import co.istad.rentiq_api.features.promotion.service.PromotionService;
import co.istad.rentiq_api.security.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/promotions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromotionController {

    private final PromotionService promotionService;

    @GetMapping
    public Page<PromotionResponse> listPromotions(
            @RequestParam(required = false) PromotionStatus status,
            @RequestParam(required = false) String vendorId,
            @RequestParam(required = false) UUID itemId,
            @RequestParam(required = false) PromotionPackage packageType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return promotionService.adminList(status, vendorId, itemId, packageType, createdFrom, createdTo, pageable);
    }

    @PatchMapping("/{id}/status")
    public PromotionResponse suspendPromotion(
            @PathVariable UUID id,
            @Valid @RequestBody SuspendPromotionRequest request
    ) {
        return promotionService.adminSuspend(id, request, AuthUtils.extractUserId());
    }
}
