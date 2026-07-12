package co.istad.rentiq_api.features.item.controller;

import co.istad.rentiq_api.features.item.dto.respone.PageResponse;
import co.istad.rentiq_api.features.item.dto.request.*;
import co.istad.rentiq_api.features.item.dto.respone.ItemResponse;
import co.istad.rentiq_api.features.item.service.ItemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/items")
    public ResponseEntity<PageResponse<ItemResponse>> getPublicItems(
            @Valid
            @ModelAttribute
            ItemFilter filter,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page number cannot be negative")
            int pageNumber,

            @RequestParam(defaultValue = "12")
            @Min(value = 1, message = "Page size must be at least 1")
            @Max(value = 100, message = "Page size cannot exceed 100")
            int pageSize,

            @RequestParam(defaultValue = "createdAt")
            String sortBy,

            @RequestParam(defaultValue = "desc")
            String sortDirection
    ) {
        return ResponseEntity.ok(
                itemService.getPublicItems(
                        filter,
                        pageNumber,
                        pageSize,
                        sortBy,
                        sortDirection
                )
        );
    }


    @GetMapping("/items/{itemId}")
    public ResponseEntity<ItemResponse> getItemById(@PathVariable UUID itemId) {
        return ResponseEntity.ok(itemService.getPublicItemById(itemId));
    }

    @PostMapping("/items")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ItemResponse> createItem(

            @Valid
            @RequestBody
            CreateItemRequest request,

            Authentication authentication
    ) {
        ItemResponse response =
                itemService.createItem(request, authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PatchMapping("/items/{itemId}")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ItemResponse> updateItem(
            @PathVariable
            UUID itemId,

            @Valid
            @RequestBody
            UpdateItemRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(itemService.updateItem(itemId, request, authentication.getName())
        );
    }


    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<Void> deleteItem(
            @PathVariable
            UUID itemId,
            Authentication authentication
    ) {
        itemService.softDeleteItem(itemId, authentication.getName());

        return ResponseEntity.noContent().build();
    }


    @PatchMapping("/items/{itemId}/availability")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ItemResponse>
    updateAvailability(

            @PathVariable
            UUID itemId,

            @Valid
            @RequestBody
            UpdateItemAvailabilityRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(itemService.updateAvailability(itemId, request.available(), authentication.getName()));
    }


    @PatchMapping("/items/{itemId}/status")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ItemResponse> updateStatus(

            @PathVariable
            UUID itemId,

            @Valid
            @RequestBody
            UpdateItemStatusRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(itemService.updateStatus(itemId, request.status(), authentication.getName()));
    }


    @GetMapping("/vendors/me/items")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<PageResponse<ItemResponse>>
    getMyItems(

            Authentication authentication,

            @RequestParam(defaultValue = "0")
            @Min(0)
            int pageNumber,

            @RequestParam(defaultValue = "12")
            @Min(1)
            @Max(100)
            int pageSize,

            @RequestParam(defaultValue = "createdAt")
            String sortBy,

            @RequestParam(defaultValue = "desc")
            String sortDirection
    ) {
        return ResponseEntity.ok(itemService.getVendorItems(authentication.getName(), pageNumber, pageSize, sortBy, sortDirection));
    }

    @GetMapping("/vendors/me/items/{itemId}")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ItemResponse> getMyItemById(

            @PathVariable
            UUID itemId,

            Authentication authentication
    ) {
        return ResponseEntity.ok(itemService.getVendorItemById(itemId, authentication.getName()));
    }


    @GetMapping("/vendors/{ownerId}/items")
    public ResponseEntity<PageResponse<ItemResponse>>
    getVendorItems(

            @PathVariable
            String ownerId,

            @RequestParam(defaultValue = "0")
            @Min(0)
            int pageNumber,

            @RequestParam(defaultValue = "12")
            @Min(1)
            @Max(100)
            int pageSize,

            @RequestParam(defaultValue = "createdAt")
            String sortBy,

            @RequestParam(defaultValue = "desc")
            String sortDirection
    ) {
        return ResponseEntity.ok(itemService.getItemsByVendor(ownerId, pageNumber, pageSize, sortBy, sortDirection)
        );
    }
}