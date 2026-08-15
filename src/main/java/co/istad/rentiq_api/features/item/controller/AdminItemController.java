package co.istad.rentiq_api.features.item.controller;

import co.istad.rentiq_api.features.item.dto.request.RejectItemRequest;
import co.istad.rentiq_api.features.item.dto.request.SetItemFeaturedRequest;
import co.istad.rentiq_api.features.item.dto.respone.ItemResponse;
import co.istad.rentiq_api.features.item.service.ItemService;
import co.istad.rentiq_api.security.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/items")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminItemController {

    private final ItemService itemService;

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
