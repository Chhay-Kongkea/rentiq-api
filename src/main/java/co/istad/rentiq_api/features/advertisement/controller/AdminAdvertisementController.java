package co.istad.rentiq_api.features.advertisement.controller;

import co.istad.rentiq_api.features.advertisement.dto.request.RejectAdvertisementRequest;
import co.istad.rentiq_api.features.advertisement.dto.response.AdvertisementResponse;
import co.istad.rentiq_api.features.advertisement.enums.AdvertisementStatus;
import co.istad.rentiq_api.features.advertisement.service.AdvertisementService;
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
@RequestMapping("/api/v1/admin/advertisements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAdvertisementController {

    private final AdvertisementService advertisementService;

    @GetMapping
    public Page<AdvertisementResponse> listAdvertisements(
            @RequestParam(required = false) AdvertisementStatus status,
            @RequestParam(required = false) String vendorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return advertisementService.adminList(status, vendorId, from, to, pageable);
    }

    @PatchMapping("/{id}/approve")
    public AdvertisementResponse approve(@PathVariable UUID id) {
        return advertisementService.adminApprove(id, AuthUtils.extractUserId());
    }

    @PatchMapping("/{id}/reject")
    public AdvertisementResponse reject(@PathVariable UUID id, @Valid @RequestBody RejectAdvertisementRequest request) {
        return advertisementService.adminReject(id, request, AuthUtils.extractUserId());
    }

    @PatchMapping("/{id}/expire")
    public AdvertisementResponse expire(@PathVariable UUID id) {
        return advertisementService.adminExpire(id, AuthUtils.extractUserId());
    }
}
