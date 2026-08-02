package co.istad.rentiq_api.features.vendorPerformance.service;

import co.istad.rentiq_api.features.vendorPerformance.dto.response.VendorModerationResponse;
import co.istad.rentiq_api.features.vendorPerformance.dto.response.VendorPerformanceResponse;

public interface VendorPerformanceService {

    VendorPerformanceResponse getPerformance(String ownerId);

    VendorModerationResponse suspend(String targetId, String reason, String adminId);

    VendorModerationResponse ban(String targetId, String reason, String adminId);

    VendorModerationResponse reinstate(String targetId, String reason, String adminId);
}
