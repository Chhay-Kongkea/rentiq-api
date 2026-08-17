package co.istad.rentiq_api.features.vendorApplication.service;

import co.istad.rentiq_api.features.vendorApplication.dto.request.RejectVendorApplicationRequest;
import co.istad.rentiq_api.features.vendorApplication.dto.request.SubmitVendorApplicationRequest;
import co.istad.rentiq_api.features.vendorApplication.dto.response.AdminVendorApplicationResponse;
import co.istad.rentiq_api.features.vendorApplication.dto.response.VendorApplicationResponse;
import co.istad.rentiq_api.features.vendorApplication.enums.VendorApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface VendorApplicationService {

    VendorApplicationResponse submit(
            String userId,
            SubmitVendorApplicationRequest request
    );

    VendorApplicationResponse getMyApplication(String userId);

    Page<AdminVendorApplicationResponse> adminList(
            VendorApplicationStatus status,
            Pageable pageable
    );

    AdminVendorApplicationResponse adminGet(UUID applicationId);

    AdminVendorApplicationResponse approve(UUID applicationId, String adminId);

    AdminVendorApplicationResponse reject(
            UUID applicationId,
            String adminId,
            RejectVendorApplicationRequest request
    );
}
