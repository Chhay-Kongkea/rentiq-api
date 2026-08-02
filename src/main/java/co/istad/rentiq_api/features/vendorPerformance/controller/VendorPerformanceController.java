package co.istad.rentiq_api.features.vendorPerformance.controller;

import co.istad.rentiq_api.features.vendorPerformance.dto.response.VendorPerformanceResponse;
import co.istad.rentiq_api.features.vendorPerformance.service.VendorPerformanceService;
import co.istad.rentiq_api.security.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vendors/me")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class VendorPerformanceController {

    private final VendorPerformanceService vendorPerformanceService;

    @GetMapping("/performance")
    public VendorPerformanceResponse getMyPerformance() {
        return vendorPerformanceService.getPerformance(AuthUtils.extractUserId());
    }
}
