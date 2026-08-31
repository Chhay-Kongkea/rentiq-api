package co.istad.rentiq_api.features.vendorApplication.service.impl;

import co.istad.rentiq_api.common.exception.InvalidStateException;
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
import co.istad.rentiq_api.features.vendorApplication.entity.VendorApplication;
import co.istad.rentiq_api.features.vendorApplication.enums.VendorApplicationStatus;
import co.istad.rentiq_api.features.vendorApplication.repository.VendorApplicationRepository;
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
class VendorApplicationServiceImplTest {

    private static final String USER_ID = "user-1";

    @Mock private VendorApplicationRepository applicationRepository;
    @Mock private UserKycRepository kycRepository;
    @Mock private UserAddressRepository userAddressRepository;
    @Mock private KeycloakRoleService keycloakRoleService;
    @Mock private AdminAuditService adminAuditService;
    @Mock private NotificationService notificationService;

    private VendorApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VendorApplicationServiceImpl(
                applicationRepository, kycRepository, userAddressRepository,
                keycloakRoleService, adminAuditService, notificationService);
        lenient().when(applicationRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(applicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private VendorApplication pendingApplication(UUID id) {
        return VendorApplication.builder().id(id).userId(USER_ID).status(VendorApplicationStatus.PENDING).build();
    }

    @Test
    void approve_approvesPendingApplication_assignsRole_andNotifies() {
        UUID applicationId = UUID.randomUUID();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(pendingApplication(applicationId)));
        when(kycRepository.findByUserId(USER_ID)).thenReturn(Optional.of(
                UserKyc.builder().userId(USER_ID).verificationStatus(KycStatus.APPROVED.name()).build()));

        service.approve(applicationId, "admin-1");

        verify(keycloakRoleService).assignRealmRole(USER_ID, RoleEnum.VENDOR);
        verify(adminAuditService).record(
                AdminAuditAction.VENDOR_APPLICATION_APPROVED,
                AdminAuditTargetType.VENDOR_APPLICATION,
                applicationId.toString(),
                Map.of("status", "PENDING"),
                Map.of("status", "APPROVED"),
                null);
        verify(notificationService).notifyUser(
                eq(USER_ID), eq(NotificationType.VENDOR_APPLICATION), any(), any(),
                eq(NotificationReferenceType.VENDOR_APPLICATION), eq(applicationId));
    }

    @Test
    void approve_rejectsWhenKycNotApproved_andDoesNotNotify() {
        UUID applicationId = UUID.randomUUID();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(pendingApplication(applicationId)));
        when(kycRepository.findByUserId(USER_ID)).thenReturn(Optional.of(
                UserKyc.builder().userId(USER_ID).verificationStatus(KycStatus.PENDING.name()).build()));

        assertThatThrownBy(() -> service.approve(applicationId, "admin-1"))
                .isInstanceOf(InvalidStateException.class);

        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any());
        verify(keycloakRoleService, never()).assignRealmRole(any(), any());
    }

    @Test
    void reject_rejectsPendingApplication_andNotifiesWithReason() {
        UUID applicationId = UUID.randomUUID();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(pendingApplication(applicationId)));

        service.reject(applicationId, "admin-1", new RejectVendorApplicationRequest("Incomplete documents"));

        verify(adminAuditService).record(
                AdminAuditAction.VENDOR_APPLICATION_REJECTED,
                AdminAuditTargetType.VENDOR_APPLICATION,
                applicationId.toString(),
                Map.of("status", "PENDING"),
                Map.of("status", "REJECTED"),
                "Incomplete documents");
        verify(notificationService).notifyUser(
                eq(USER_ID), eq(NotificationType.VENDOR_APPLICATION), any(), any(),
                eq(NotificationReferenceType.VENDOR_APPLICATION), eq(applicationId));
    }

    @Test
    void reject_rejectsAlreadyReviewedApplication_andDoesNotNotify() {
        UUID applicationId = UUID.randomUUID();
        VendorApplication approved = pendingApplication(applicationId);
        approved.setStatus(VendorApplicationStatus.APPROVED);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> service.reject(applicationId, "admin-1", new RejectVendorApplicationRequest("reason")))
                .isInstanceOf(InvalidStateException.class);

        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any());
    }
}
