package co.istad.rentiq_api.features.vendorApplication.controller;

import co.istad.rentiq_api.features.vendorApplication.dto.request.RejectVendorApplicationRequest;
import co.istad.rentiq_api.features.vendorApplication.dto.response.AdminVendorApplicationResponse;
import co.istad.rentiq_api.features.vendorApplication.enums.VendorApplicationStatus;
import co.istad.rentiq_api.features.vendorApplication.service.VendorApplicationService;
import co.istad.rentiq_api.security.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/vendor-applications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminVendorApplicationController {

    private final VendorApplicationService vendorApplicationService;

    @GetMapping
    public ResponseEntity<Page<AdminVendorApplicationResponse>> list(
            @RequestParam(required = false) VendorApplicationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(vendorApplicationService.adminList(status, pageable));
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<AdminVendorApplicationResponse> get(
            @PathVariable UUID applicationId
    ) {
        return ResponseEntity.ok(vendorApplicationService.adminGet(applicationId));
    }

    @PatchMapping("/{applicationId}/approve")
    public ResponseEntity<AdminVendorApplicationResponse> approve(
            @PathVariable UUID applicationId
    ) {
        return ResponseEntity.ok(
                vendorApplicationService.approve(applicationId, AuthUtils.extractUserId())
        );
    }

    @PatchMapping("/{applicationId}/reject")
    public ResponseEntity<AdminVendorApplicationResponse> reject(
            @PathVariable UUID applicationId,
            @Valid @RequestBody RejectVendorApplicationRequest request
    ) {
        return ResponseEntity.ok(
                vendorApplicationService.reject(
                        applicationId,
                        AuthUtils.extractUserId(),
                        request
                )
        );
    }
}
