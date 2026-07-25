package co.istad.rentiq_api.features.item.controller;

import co.istad.rentiq_api.features.item.dto.request.CreateAvailabilityBlockRequest;
import co.istad.rentiq_api.features.item.dto.respone.AvailabilityBlockResponse;
import co.istad.rentiq_api.features.item.service.ItemAvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/items/{itemId}")
@RequiredArgsConstructor
public class ItemAvailabilityController {

    private final ItemAvailabilityService itemAvailabilityService;

    @GetMapping("/availability")
    public ResponseEntity<List<AvailabilityBlockResponse>> getBlockedRanges(
            @PathVariable UUID itemId
    ) {
        return ResponseEntity.ok(itemAvailabilityService.getBlockedRanges(itemId));
    }

    @PostMapping("/availability-block")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<AvailabilityBlockResponse> createBlock(
            @PathVariable UUID itemId,

            @Valid
            @RequestBody
            CreateAvailabilityBlockRequest request,

            Authentication authentication
    ) {
        AvailabilityBlockResponse response =
                itemAvailabilityService.createBlock(itemId, request, authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/availability-block/{blockId}")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<Void> deleteBlock(
            @PathVariable UUID itemId,

            @PathVariable UUID blockId,

            Authentication authentication
    ) {
        itemAvailabilityService.deleteBlock(itemId, blockId, authentication.getName());

        return ResponseEntity.noContent().build();
    }
}
