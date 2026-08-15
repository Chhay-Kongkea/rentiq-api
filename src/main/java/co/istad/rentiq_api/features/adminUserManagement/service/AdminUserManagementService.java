package co.istad.rentiq_api.features.adminUserManagement.service;

import co.istad.rentiq_api.features.adminUserManagement.dto.response.AdminUserResponse;
import co.istad.rentiq_api.features.adminUserManagement.dto.response.AdminUserStatusResponse;
import co.istad.rentiq_api.features.adminUserManagement.dto.response.AdminVendorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserManagementService {

    Page<AdminUserResponse> listUsers(Pageable pageable);

    AdminUserResponse getUser(String userId);

    AdminUserStatusResponse suspendUser(String userId, String reason, String adminId);

    AdminUserStatusResponse banUser(String userId, String reason, String adminId);

    AdminUserStatusResponse reinstateUser(String userId, String reason, String adminId);

    Page<AdminVendorResponse> listVendors(Pageable pageable);

    AdminVendorResponse getVendor(String userId);
}
