package co.istad.rentiq_api.features.vendorApplication.service.impl;

import co.istad.rentiq_api.common.exception.DuplicateException;
import co.istad.rentiq_api.common.exception.InvalidStateException;
import co.istad.rentiq_api.common.exception.NotFoundException;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
import co.istad.rentiq_api.features.auth.RoleEnum;
import co.istad.rentiq_api.features.auth.service.KeycloakRoleService;
import co.istad.rentiq_api.features.kyc.KycStatus;
import co.istad.rentiq_api.features.kyc.entity.UserKyc;
import co.istad.rentiq_api.features.kyc.repository.UserKycRepository;
import co.istad.rentiq_api.features.notification.enums.NotificationReferenceType;
import co.istad.rentiq_api.features.notification.enums.NotificationType;
import co.istad.rentiq_api.features.notification.service.NotificationService;
import co.istad.rentiq_api.features.userProfile.repository.UserAddressRepository;
import co.istad.rentiq_api.features.vendorApplication.dto.request.RejectVendorApplicationRequest;
import co.istad.rentiq_api.features.vendorApplication.dto.request.SubmitVendorApplicationRequest;
import co.istad.rentiq_api.features.vendorApplication.dto.response.AdminVendorApplicationResponse;
import co.istad.rentiq_api.features.vendorApplication.dto.response.VendorApplicationResponse;
import co.istad.rentiq_api.features.vendorApplication.entity.VendorApplication;
import co.istad.rentiq_api.features.vendorApplication.enums.VendorApplicationStatus;
import co.istad.rentiq_api.features.vendorApplication.repository.VendorApplicationRepository;
import co.istad.rentiq_api.features.vendorApplication.service.VendorApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorApplicationServiceImpl implements VendorApplicationService {

    private final VendorApplicationRepository applicationRepository;
    private final UserKycRepository kycRepository;
    private final UserAddressRepository userAddressRepository;
    private final KeycloakRoleService keycloakRoleService;
    private final AdminAuditService adminAuditService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public VendorApplicationResponse submit(
            String userId,
            SubmitVendorApplicationRequest request
    ) {
        if (keycloakRoleService.hasRealmRole(userId, RoleEnum.VENDOR)) {
            throw new InvalidStateException(
                    "Vendor application",
                    "VENDOR",
                    "This account is already a vendor"
            );
        }

        requireSubmittedKyc(userId);
        requireDefaultAddress(userId);

        VendorApplication application = applicationRepository
                .findByUserId(userId)
                .map(existing -> prepareResubmission(existing, request))
                .orElseGet(() -> VendorApplication.builder()
                        .userId(userId)
                        .message(request == null ? null : request.message())
                        .status(VendorApplicationStatus.PENDING)
                        .build());

        VendorApplication saved = applicationRepository.save(application);
        return toUserResponse(saved);
    }

    @Override
    public VendorApplicationResponse getMyApplication(String userId) {
        return toUserResponse(
                applicationRepository.findByUserId(userId)
                        .orElseThrow(() -> new NotFoundException(
                                "Vendor application",
                                "userId",
                                userId
                        ))
        );
    }

    @Override
    public Page<AdminVendorApplicationResponse> adminList(
            VendorApplicationStatus status,
            Pageable pageable
    ) {
        Page<VendorApplication> page = status == null
                ? applicationRepository.findAll(pageable)
                : applicationRepository.findAllByStatus(status, pageable);

        return page.map(this::toAdminResponse);
    }

    @Override
    public AdminVendorApplicationResponse adminGet(UUID applicationId) {
        return toAdminResponse(requireApplication(applicationId));
    }

    @Override
    @Transactional
    public AdminVendorApplicationResponse approve(UUID applicationId, String adminId) {
        VendorApplication application = requireApplication(applicationId);
        requirePending(application);
        requireApprovedKyc(application.getUserId());

        application.setStatus(VendorApplicationStatus.APPROVED);
        application.setRejectionReason(null);
        application.setReviewedBy(adminId);
        application.setReviewedAt(OffsetDateTime.now());

        // Flush the DB state first. If Keycloak fails, the surrounding transaction
        // is rolled back and the application stays PENDING.
        VendorApplication saved = applicationRepository.saveAndFlush(application);

        keycloakRoleService.assignRealmRole(saved.getUserId(), RoleEnum.VENDOR);

        adminAuditService.record(
                AdminAuditAction.VENDOR_APPLICATION_APPROVED,
                AdminAuditTargetType.VENDOR_APPLICATION,
                saved.getId().toString(),
                Map.of("status", VendorApplicationStatus.PENDING.name()),
                Map.of("status", VendorApplicationStatus.APPROVED.name()),
                null);

        notificationService.notifyUser(
                saved.getUserId(),
                NotificationType.VENDOR_APPLICATION,
                "Application approved",
                "Your vendor application has been approved.",
                NotificationReferenceType.VENDOR_APPLICATION,
                saved.getId());

        return toAdminResponse(saved);
    }

    @Override
    @Transactional
    public AdminVendorApplicationResponse reject(
            UUID applicationId,
            String adminId,
            RejectVendorApplicationRequest request
    ) {
        VendorApplication application = requireApplication(applicationId);
        requirePending(application);

        application.setStatus(VendorApplicationStatus.REJECTED);
        application.setRejectionReason(request.reason());
        application.setReviewedBy(adminId);
        application.setReviewedAt(OffsetDateTime.now());

        VendorApplication saved = applicationRepository.save(application);

        adminAuditService.record(
                AdminAuditAction.VENDOR_APPLICATION_REJECTED,
                AdminAuditTargetType.VENDOR_APPLICATION,
                saved.getId().toString(),
                Map.of("status", VendorApplicationStatus.PENDING.name()),
                Map.of("status", VendorApplicationStatus.REJECTED.name()),
                request.reason());

        notificationService.notifyUser(
                saved.getUserId(),
                NotificationType.VENDOR_APPLICATION,
                "Application rejected",
                "Your vendor application was not approved. Please review the reason and update your application.",
                NotificationReferenceType.VENDOR_APPLICATION,
                saved.getId());

        return toAdminResponse(saved);
    }

    private VendorApplication prepareResubmission(
            VendorApplication existing,
            SubmitVendorApplicationRequest request
    ) {
        if (existing.getStatus() == VendorApplicationStatus.PENDING) {
            throw new DuplicateException(
                    "Vendor application",
                    "A vendor application is already pending for this account"
            );
        }

        if (existing.getStatus() == VendorApplicationStatus.APPROVED) {
            throw new InvalidStateException(
                    "Vendor application",
                    existing.getStatus(),
                    "An approved vendor application cannot be submitted again"
            );
        }

        existing.setStatus(VendorApplicationStatus.PENDING);
        existing.setMessage(request == null ? null : request.message());
        existing.setRejectionReason(null);
        existing.setReviewedBy(null);
        existing.setReviewedAt(null);

        return existing;
    }

    private UserKyc requireSubmittedKyc(String userId) {
        return kycRepository.findByUserId(userId)
                .orElseThrow(() -> new InvalidStateException(
                        "Vendor application",
                        "KYC_NOT_SUBMITTED",
                        "Please submit KYC before applying to become a vendor"
                ));
    }

    private void requireDefaultAddress(String userId) {
        userAddressRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new InvalidStateException(
                        "Vendor application",
                        "DEFAULT_ADDRESS_REQUIRED",
                        "Please add a default address before applying to become a vendor"
                ));
    }

    private void requireApprovedKyc(String userId) {
        UserKyc kyc = requireSubmittedKyc(userId);

        if (!KycStatus.APPROVED.name().equals(kyc.getVerificationStatus())) {
            throw new InvalidStateException(
                    "Vendor application",
                    kyc.getVerificationStatus(),
                    "KYC must be approved before the vendor application can be approved"
            );
        }
    }

    private VendorApplication requireApplication(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException(
                        "Vendor application",
                        applicationId
                ));
    }

    private void requirePending(VendorApplication application) {
        if (application.getStatus() != VendorApplicationStatus.PENDING) {
            throw new InvalidStateException(
                    "Vendor application",
                    application.getStatus(),
                    "Only a pending vendor application can be reviewed"
            );
        }
    }

    private VendorApplicationResponse toUserResponse(VendorApplication application) {
        return new VendorApplicationResponse(
                application.getId(),
                application.getStatus(),
                application.getMessage(),
                application.getRejectionReason(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }

    private AdminVendorApplicationResponse toAdminResponse(VendorApplication application) {
        return new AdminVendorApplicationResponse(
                application.getId(),
                application.getUserId(),
                application.getStatus(),
                application.getMessage(),
                application.getRejectionReason(),
                application.getReviewedBy(),
                application.getReviewedAt(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}
