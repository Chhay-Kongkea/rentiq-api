package co.istad.rentiq_api.features.kyc.service.impl;

import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
import co.istad.rentiq_api.features.notification.enums.NotificationReferenceType;
import co.istad.rentiq_api.features.notification.enums.NotificationType;
import co.istad.rentiq_api.features.notification.service.NotificationService;
import co.istad.rentiq_api.features.auth.service.AuthService;
import co.istad.rentiq_api.features.kyc.KycStatus;
import co.istad.rentiq_api.features.kyc.dto.request.RejectKycRequest;
import co.istad.rentiq_api.features.kyc.entity.UserKyc;
import co.istad.rentiq_api.features.kyc.exception.InvalidKycStatusException;
import co.istad.rentiq_api.features.kyc.mapper.KycMapper;
import co.istad.rentiq_api.features.kyc.repository.UserKycRepository;
import co.istad.rentiq_api.features.kyc.service.KycImageStorageService;
import co.istad.rentiq_api.features.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KycServiceImplTest {

    @Mock private UserKycRepository kycRepository;
    @Mock private KycImageStorageService imageStorageService;
    @Mock private AuthService authService;
    @Mock private WalletService walletService;
    @Mock private KycMapper kycMapper;
    @Mock private AdminAuditService adminAuditService;
    @Mock private NotificationService notificationService;

    private KycServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new KycServiceImpl(kycRepository, imageStorageService, authService, walletService, kycMapper, adminAuditService, notificationService);
        lenient().when(kycRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private UserKyc pendingKyc(UUID id) {
        return UserKyc.builder()
                .id(id)
                .userId("user-1")
                .verificationStatus(KycStatus.PENDING.name())
                .build();
    }

    @Test
    void adminApproveKyc_approvesPendingKyc_andRecordsAudit() {
        UUID kycId = UUID.randomUUID();
        when(kycRepository.findById(kycId)).thenReturn(Optional.of(pendingKyc(kycId)));

        service.adminApproveKyc(kycId, "admin-1");

        verify(adminAuditService).record(
                AdminAuditAction.KYC_APPROVED,
                AdminAuditTargetType.KYC,
                kycId.toString(),
                Map.of("status", "PENDING"),
                Map.of("status", "APPROVED"),
                null);
        verify(walletService).grantWelcomeBonusIfEligible("user-1");
        verify(notificationService).notifyUser(
                eq("user-1"), eq(NotificationType.KYC), any(), any(), eq(NotificationReferenceType.KYC), eq(kycId));
    }

    @Test
    void adminApproveKyc_rejectsAlreadyApprovedKyc_andDoesNotRecordAudit() {
        UUID kycId = UUID.randomUUID();
        UserKyc approved = pendingKyc(kycId);
        approved.setVerificationStatus(KycStatus.APPROVED.name());
        when(kycRepository.findById(kycId)).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> service.adminApproveKyc(kycId, "admin-1"))
                .isInstanceOf(InvalidKycStatusException.class);

        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any());
    }

    @Test
    void adminRejectKyc_rejectsPendingKyc_andRecordsAuditWithReason() {
        UUID kycId = UUID.randomUUID();
        when(kycRepository.findById(kycId)).thenReturn(Optional.of(pendingKyc(kycId)));

        service.adminRejectKyc(kycId, "admin-1", new RejectKycRequest("Blurry ID photo"));

        verify(adminAuditService).record(
                AdminAuditAction.KYC_REJECTED,
                AdminAuditTargetType.KYC,
                kycId.toString(),
                Map.of("status", "PENDING"),
                Map.of("status", "REJECTED"),
                "Blurry ID photo");
        verify(notificationService).notifyUser(
                eq("user-1"), eq(NotificationType.KYC), any(), any(), eq(NotificationReferenceType.KYC), eq(kycId));
    }
}
