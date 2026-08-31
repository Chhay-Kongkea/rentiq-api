package co.istad.rentiq_api.features.promotion.controller;

import co.istad.rentiq_api.features.promotion.dto.request.CreatePromotionRequest;
import co.istad.rentiq_api.features.promotion.dto.response.PromotionResponse;
import co.istad.rentiq_api.features.promotion.dto.response.PromotionStatsResponse;
import co.istad.rentiq_api.features.promotion.enums.PromotionStatus;
import co.istad.rentiq_api.features.promotion.service.PromotionService;
import co.istad.rentiq_api.security.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping("/promotions")
    @PreAuthorize("hasRole('VENDOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public PromotionResponse createPromotion(@Valid @RequestBody CreatePromotionRequest request) {
        return promotionService.create(request, AuthUtils.extractUserId());
    }

    @GetMapping("/promotions/{id}")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    public PromotionResponse getPromotion(@PathVariable UUID id, Authentication authentication) {
        return promotionService.getById(id, authentication.getName(), isAdmin(authentication));
    }

    @PatchMapping("/promotions/{id}/cancel")
    @PreAuthorize("hasRole('VENDOR')")
    public PromotionResponse cancelPromotion(@PathVariable UUID id) {
        return promotionService.cancel(id, AuthUtils.extractUserId());
    }

    @GetMapping("/promotions/{id}/stats")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    public PromotionStatsResponse getPromotionStats(@PathVariable UUID id, Authentication authentication) {
        return promotionService.getStats(id, authentication.getName(), isAdmin(authentication));
    }

    @PostMapping("/promotions/{id}/impression")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordImpression(@PathVariable UUID id) {
        promotionService.recordImpression(id);
    }

    @PostMapping("/promotions/{id}/click")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordClick(@PathVariable UUID id) {
        promotionService.recordClick(id);
    }

    @GetMapping("/vendors/me/promotions")
    @PreAuthorize("hasRole('VENDOR')")
    public Page<PromotionResponse> getMyPromotions(
            @RequestParam(required = false) PromotionStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return promotionService.getMyPromotions(AuthUtils.extractUserId(), status, pageable);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
