package co.istad.rentiq_api.features.kyc.service;


import co.istad.rentiq_api.features.kyc.dto.request.*;
import co.istad.rentiq_api.features.kyc.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface KycService {

    KycResponse submitKyc(String userId, SubmitKycRequest request,
                          MultipartFile frontImage, MultipartFile backImage);

    KycResponse getMyKyc(String userId);

    KycResponse resubmitKyc(String userId, SubmitKycRequest request,
                            MultipartFile frontImage, MultipartFile backImage);

    void startEmailVerification(String userId);

    KycResponse confirmEmailVerification(String userId);

    Page<AdminKycListItemResponse> adminListKyc(String status, Pageable pageable);

    AdminKycDetailResponse adminGetKyc(UUID kycId);

    AdminKycDetailResponse adminApproveKyc(UUID kycId, String adminId);

    AdminKycDetailResponse adminRejectKyc(UUID kycId, String adminId, RejectKycRequest request);
}