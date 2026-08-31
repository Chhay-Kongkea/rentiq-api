package co.istad.rentiq_api.features.item.controller;

import co.istad.rentiq_api.features.item.dto.request.RejectItemRequest;
import co.istad.rentiq_api.features.item.dto.request.AdminItemFilter;
import co.istad.rentiq_api.features.item.dto.request.SetItemFeaturedRequest;
import co.istad.rentiq_api.features.item.dto.respone.AdminItemResponse;
import co.istad.rentiq_api.features.item.dto.respone.ItemResponse;
import co.istad.rentiq_api.features.item.dto.respone.PageResponse;
import co.istad.rentiq_api.features.item.service.ItemService;
import co.istad.rentiq_api.security.AuthUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/items")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminItemController {

    private final ItemService itemService;

    @GetMapping
    public ResponseEntity<PageResponse<AdminItemResponse>> getItems(
            @Valid @ModelAttribute AdminItemFilter filter,
            @RequestParam(defaultValue = "0") @Min(0) int pageNumber,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        return ResponseEntity.ok(itemService.getAdminItems(
                filter, pageNumber, pageSize, sortBy, sortDirection
        ));
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<AdminItemResponse> getItem(@PathVariable UUID itemId) {
        return ResponseEntity.ok(itemService.getAdminItemById(itemId));
    }

    @PostMapping("/{itemId}/approve")
    public ResponseEntity<ItemResponse> approveItem(@PathVariable UUID itemId) {
        return ResponseEntity.ok(
                itemService.adminApproveItem(itemId, AuthUtils.extractUserId())
        );
    }

    @PostMapping("/{itemId}/reject")
    public ResponseEntity<ItemResponse> rejectItem(
            @PathVariable UUID itemId,

            @Valid
            @RequestBody
            RejectItemRequest request
    ) {
        return ResponseEntity.ok(
                itemService.adminRejectItem(itemId, request.reason(), AuthUtils.extractUserId())
        );
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> removeItem(@PathVariable UUID itemId) {
        itemService.adminRemoveItem(itemId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{itemId}/featured")
    public ResponseEntity<ItemResponse> setFeatured(
            @PathVariable UUID itemId,

            @Valid
            @RequestBody
            SetItemFeaturedRequest request
    ) {
        return ResponseEntity.ok(
                itemService.adminSetFeatured(itemId, request.featured(), request.featuredUntil())
        );
    }
}
