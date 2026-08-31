package co.istad.rentiq_api.features.kyc.service.impl;


import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
import co.istad.rentiq_api.features.auth.dto.request.ResendVerificationRequest;
import co.istad.rentiq_api.features.auth.service.AuthService;
import co.istad.rentiq_api.features.kyc.KycStatus;

import co.istad.rentiq_api.features.kyc.dto.request.*;
import co.istad.rentiq_api.features.kyc.dto.response.*;
import co.istad.rentiq_api.features.kyc.entity.UserKyc;
import co.istad.rentiq_api.features.kyc.exception.*;
import co.istad.rentiq_api.features.kyc.mapper.KycMapper;
import co.istad.rentiq_api.features.kyc.repository.UserKycRepository;
import co.istad.rentiq_api.features.kyc.service.KycImageStorageService;
import co.istad.rentiq_api.features.kyc.service.KycService;
import co.istad.rentiq_api.features.notification.enums.NotificationReferenceType;
import co.istad.rentiq_api.features.notification.enums.NotificationType;
import co.istad.rentiq_api.features.notification.service.NotificationService;

import co.istad.rentiq_api.features.wallet.service.WalletService;
import co.istad.rentiq_api.security.AuthUtils;
import jakarta.ws.rs.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycServiceImpl implements KycService {

    private final UserKycRepository kycRepository;
    private final KycImageStorageService imageStorageService;
    private final AuthService authService;
    private final WalletService walletService;
    private final KycMapper kycMapper;
    private final AdminAuditService adminAuditService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public KycResponse submitKyc(String userId, SubmitKycRequest request,
                                 MultipartFile frontImage, MultipartFile backImage) {

        if (kycRepository.existsByUserId(userId)) {
            throw new KycAlreadySubmittedException();
        }

        String frontUrl = imageStorageService.upload(userId, "front", frontImage);
        String backUrl = imageStorageService.upload(userId, "back", backImage);

        UserKyc kyc = UserKyc.builder()
                .userId(userId)
                .nationalIdNumber(request.nationalIdNumber())
                .nationalIdType(request.nationalIdType())
                .nationalIdCountry(request.nationalIdCountry() != null ? request.nationalIdCountry() : "KHM")
                .frontImageUrl(frontUrl)
                .backImageUrl(backUrl)
                .verificationStatus(KycStatus.PENDING.name())
                .build();

        kyc = kycRepository.save(kyc);
        log.info("KYC submitted for user {}", userId);

        return kycMapper.toResponse(kyc);
    }

    @Override
    public KycResponse getMyKyc(String userId) {
        return kycMapper.toResponse(findByUserId(userId));
    }

    @Override
    @Transactional
    public KycResponse resubmitKyc(String userId, SubmitKycRequest request,
                                   MultipartFile frontImage, MultipartFile backImage) {

        UserKyc kyc = findByUserId(userId);

        if (KycStatus.APPROVED.name().equals(kyc.getVerificationStatus())) {
            throw new InvalidKycStatusException("Cannot resubmit an already approved KYC");
        }

        kyc.setNationalIdNumber(request.nationalIdNumber());
        kyc.setNationalIdType(request.nationalIdType());
        if (request.nationalIdCountry() != null) {
            kyc.setNationalIdCountry(request.nationalIdCountry());
        }

        if (frontImage != null && !frontImage.isEmpty()) {
            kyc.setFrontImageUrl(imageStorageService.upload(userId, "front", frontImage));
        }
        if (backImage != null && !backImage.isEmpty()) {
            kyc.setBackImageUrl(imageStorageService.upload(userId, "back", backImage));
        }

        // Resubmission resets status back to PENDING and clears any prior rejection.
        kyc.setVerificationStatus(KycStatus.PENDING.name());
        kyc.setRejectionReason(null);
        kyc.setReviewedBy(null);
        kyc.setReviewedAt(null);

        kyc = kycRepository.save(kyc);
        log.info("KYC resubmitted for user {}", userId);

        return kycMapper.toResponse(kyc);
    }

    // ---------------------------------------------------------------
    // PHONE VERIFICATION
    // ---------------------------------------------------------------

//    @Override
//    @Transactional
//    public void startPhoneVerification(String userId, StartPhoneVerificationRequest request) {
//
//        UserKyc kyc = findByUserId(userId);
//        kyc.setPhoneNumber(request.phoneNumber());
//        kycRepository.save(kyc);
//
//        otpService.generateAndSend(userId, request.phoneNumber());
//    }
//
//    @Override
//    @Transactional
//    public KycResponse confirmPhoneOtp(String userId, ConfirmOtpRequest request) {
//
//        UserKyc kyc = findByUserId(userId);
//
//        otpService.verify(userId, request.code());
//
//        kyc.setPhoneVerified(true);
//        kyc = kycRepository.save(kyc);
//
//        log.info("Phone verified for user {}", userId);
//        return toResponse(kyc);
//    }

    // ---------------------------------------------------------------
    // EMAIL VERIFICATION (delegates to AuthService — no duplicate logic)
    // ---------------------------------------------------------------

    @Override
    public void startEmailVerification(String userId) {

        UserKyc kyc = findByUserId(userId);

        if (kyc.isEmailVerified()) {
            throw new BadRequestException("Email already verified");
        }

        String email = AuthUtils.extractEmail();

        authService.resendVerificationEmail(
                new ResendVerificationRequest(email)
        );

        log.info("Email verification sent for user {}", userId);
    }

    @Override
    @Transactional
    public KycResponse confirmEmailVerification(String userId) {

        UserKyc kyc = findByUserId(userId);
        String email = AuthUtils.extractEmail();

        boolean verified = authService.isEmailVerified(email);
        kyc.setEmailVerified(verified);
        kyc = kycRepository.save(kyc);

        log.info("Email verification synced for user {}: {}", userId, verified);
        return kycMapper.toResponse(kyc);
    }

    // ---------------------------------------------------------------
    // ADMIN
    // ---------------------------------------------------------------

    @Override
    public Page<AdminKycListItemResponse> adminListKyc(String status, Pageable pageable) {

        Page<UserKyc> page = (status != null && !status.isBlank())
                ? kycRepository.findAllByVerificationStatus(status.toUpperCase(), pageable)
                : kycRepository.findAll(pageable);

        return page.map(kyc -> AdminKycListItemResponse.builder()
                .id(kyc.getId())
                .userId(kyc.getUserId())
                .nationalIdNumber(kyc.getNationalIdNumber())
                .verificationStatus(kyc.getVerificationStatus())
                .emailVerified(kyc.isEmailVerified())
                .createdAt(kyc.getCreatedAt())
                .build());
    }

    @Override
    public AdminKycDetailResponse adminGetKyc(UUID kycId) {
        return kycMapper.toAdminDetailResponse(findById(kycId));
    }

    @Override
    @Transactional
    public AdminKycDetailResponse adminApproveKyc(UUID kycId, String adminId) {

        UserKyc kyc = findById(kycId);

        if (KycStatus.APPROVED.name().equals(kyc.getVerificationStatus())) {
            throw new InvalidKycStatusException("KYC is already approved");
        }

        String previousStatus = kyc.getVerificationStatus();

        kyc.setVerificationStatus(KycStatus.APPROVED.name());
        kyc.setVerifiedAt(OffsetDateTime.now());
        kyc.setReviewedBy(adminId);
        kyc.setReviewedAt(OffsetDateTime.now());
        kyc.setRejectionReason(null);

        kyc = kycRepository.save(kyc);

        walletService.grantWelcomeBonusIfEligible(kyc.getUserId());

        adminAuditService.record(
                AdminAuditAction.KYC_APPROVED,
                AdminAuditTargetType.KYC,
                kyc.getId().toString(),
                Map.of("status", previousStatus),
                Map.of("status", KycStatus.APPROVED.name()),
                null);

        notificationService.notifyUser(
                kyc.getUserId(),
                NotificationType.KYC,
                "KYC approved",
                "Your identity verification has been approved.",
                NotificationReferenceType.KYC,
                kyc.getId());

        log.info("KYC {} approved by admin {}", kycId, adminId);
        return kycMapper.toAdminDetailResponse(kyc);
    }

    @Override
    @Transactional
    public AdminKycDetailResponse adminRejectKyc(UUID kycId, String adminId, RejectKycRequest request) {

        UserKyc kyc = findById(kycId);

        if (KycStatus.APPROVED.name().equals(kyc.getVerificationStatus())) {
            throw new InvalidKycStatusException("Cannot reject an already approved KYC");
        }

        String previousStatus = kyc.getVerificationStatus();

        kyc.setVerificationStatus(KycStatus.REJECTED.name());
        kyc.setRejectionReason(request.reason());
        kyc.setReviewedBy(adminId);
        kyc.setReviewedAt(OffsetDateTime.now());

        kyc = kycRepository.save(kyc);

        adminAuditService.record(
                AdminAuditAction.KYC_REJECTED,
                AdminAuditTargetType.KYC,
                kyc.getId().toString(),
                Map.of("status", previousStatus),
                Map.of("status", KycStatus.REJECTED.name()),
                request.reason());

        notificationService.notifyUser(
                kyc.getUserId(),
                NotificationType.KYC,
                "KYC rejected",
                "Your identity verification was rejected. Please review the reason and resubmit.",
                NotificationReferenceType.KYC,
                kyc.getId());

        log.info("KYC {} rejected by admin {}", kycId, adminId);
        return kycMapper.toAdminDetailResponse(kyc);
    }

    // ---------------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------------

    private UserKyc findByUserId(String userId) {
        return kycRepository.findByUserId(userId).orElseThrow(KycNotFoundException::new);
    }

    private UserKyc findById(UUID id) {
        return kycRepository.findById(id).orElseThrow(KycNotFoundException::new);
    }

}