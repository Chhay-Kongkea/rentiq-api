package co.istad.rentiq_api.features.vendorPerformance.controller;

import co.istad.rentiq_api.features.adminUserManagement.dto.response.AdminVendorResponse;
import co.istad.rentiq_api.features.adminUserManagement.service.AdminUserManagementService;
import co.istad.rentiq_api.features.vendorPerformance.dto.request.VendorModerationRequest;
import co.istad.rentiq_api.features.vendorPerformance.dto.response.VendorModerationResponse;
import co.istad.rentiq_api.features.vendorPerformance.dto.response.VendorPerformanceResponse;
import co.istad.rentiq_api.features.vendorPerformance.service.VendorPerformanceService;
import co.istad.rentiq_api.security.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/vendors")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminVendorController {

    private final VendorPerformanceService vendorPerformanceService;
    private final AdminUserManagementService adminUserManagementService;

    @GetMapping
    public Page<AdminVendorResponse> listVendors(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return adminUserManagementService.listVendors(pageable);
    }

    @GetMapping("/{id}")
    public AdminVendorResponse getVendor(@PathVariable String id) {
        return adminUserManagementService.getVendor(id);
    }

    @GetMapping("/{ownerId}/performance")
    public VendorPerformanceResponse getVendorPerformance(@PathVariable String ownerId) {
        return vendorPerformanceService.getPerformance(ownerId);
    }

    @PostMapping("/{ownerId}/suspend")
    public VendorModerationResponse suspendVendor(
            @PathVariable String ownerId,
            @Valid @RequestBody VendorModerationRequest request
    ) {
        return vendorPerformanceService.suspend(ownerId, request.reason(), AuthUtils.extractUserId());
    }

    @PostMapping("/{ownerId}/ban")
    public VendorModerationResponse banVendor(
            @PathVariable String ownerId,
            @Valid @RequestBody VendorModerationRequest request
    ) {
        return vendorPerformanceService.ban(ownerId, request.reason(), AuthUtils.extractUserId());
    }

    @PostMapping("/{ownerId}/reinstate")
    public VendorModerationResponse reinstateVendor(
            @PathVariable String ownerId,
            @Valid @RequestBody VendorModerationRequest request
    ) {
        return vendorPerformanceService.reinstate(ownerId, request.reason(), AuthUtils.extractUserId());
    }
}
