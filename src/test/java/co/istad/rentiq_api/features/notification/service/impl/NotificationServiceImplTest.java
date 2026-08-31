package co.istad.rentiq_api.features.notification.service.impl;

import co.istad.rentiq_api.common.exception.InvalidOperationException;
import co.istad.rentiq_api.common.exception.NotFoundException;
import co.istad.rentiq_api.features.notification.dto.response.AdminNotificationResponse;
import co.istad.rentiq_api.features.notification.dto.response.NotificationResponse;
import co.istad.rentiq_api.features.notification.dto.request.BroadcastNotificationRequest;
import co.istad.rentiq_api.features.notification.enums.BroadcastAudienceType;
import co.istad.rentiq_api.features.userProfile.repository.UserRepository;
import co.istad.rentiq_api.features.userProfile.entity.User;
import co.istad.rentiq_api.features.userProfile.enums.AccountStatus;
import co.istad.rentiq_api.common.config.props.KeycloakAdminClientProps;
import co.istad.rentiq_api.features.notification.entity.Notification;
import co.istad.rentiq_api.features.notification.enums.NotificationReferenceType;
import co.istad.rentiq_api.features.notification.enums.NotificationType;
import co.istad.rentiq_api.features.notification.mapper.NotificationMapper;
import co.istad.rentiq_api.features.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    private static final String RECIPIENT_ID = "user-1";

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationMapper notificationMapper;
    @Mock private UserRepository userRepository;

    private Keycloak keycloak;
    private KeycloakAdminClientProps keycloakProps;

    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        keycloak = mock(Keycloak.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        keycloakProps = new KeycloakAdminClientProps();
        keycloakProps.setTargetRealm("rentiq");
        service = new NotificationServiceImpl(
                notificationRepository, notificationMapper, userRepository,
                keycloak, keycloakProps);
    }

    // ---------------------------------------------------------------
    // notifyUser — internal persistence
    // ---------------------------------------------------------------

    @Test
    void notifyUser_persistsUnreadNotificationForRecipient() {
        UUID referenceId = UUID.randomUUID();
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        when(notificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.notifyUser(
                RECIPIENT_ID,
                NotificationType.KYC,
                "KYC approved",
                "Your identity verification has been approved.",
                NotificationReferenceType.KYC,
                referenceId);

        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(RECIPIENT_ID);
        assertThat(saved.getNotificationType()).isEqualTo(NotificationType.SYSTEM);
        assertThat(saved.getTitle()).isEqualTo("KYC approved");
        assertThat(saved.getBody()).isEqualTo("Your identity verification has been approved.");
        assertThat(saved.getReferenceType()).isNull();
        assertThat(saved.getReferenceId()).isEqualTo(referenceId);
        assertThat(saved.getPayload()).containsEntry("eventType", "KYC");
        assertThat(saved.getPayload()).containsEntry("status", "APPROVED");
        assertThat(saved.getPayload()).containsEntry("referenceType", "KYC");
        assertThat(saved.getPayload()).containsEntry("referenceId", referenceId.toString());
        assertThat(saved.isRead()).isFalse();
    }

    @Test
    void getMyNotifications_queriesOnlyAuthenticatedRecipient() {
        PageRequest pageable = PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        Notification notification = Notification.builder().userId(RECIPIENT_ID).build();
        NotificationResponse response = mock(NotificationResponse.class);
        when(notificationRepository.findAllByUserIdOrderByCreatedAtDesc(RECIPIENT_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(notification), pageable, 1));
        when(notificationMapper.toResponse(notification)).thenReturn(response);

        assertThat(service.getMyNotifications(RECIPIENT_ID, 0, 20).content()).containsExactly(response);
        verify(notificationRepository).findAllByUserIdOrderByCreatedAtDesc(RECIPIENT_ID, pageable);
    }

    @Test
    void getMyNotification_rejectsForeignNotificationAsNotFound() {
        UUID id = UUID.randomUUID();
        when(notificationRepository.findByIdAndUserId(id, RECIPIENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyNotification(id, RECIPIENT_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void markAsRead_updatesOnlyOwnedNotification() {
        UUID id = UUID.randomUUID();
        Notification notification = Notification.builder().id(id).userId(RECIPIENT_ID).build();
        when(notificationRepository.findByIdAndUserId(id, RECIPIENT_ID)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        service.markAsRead(id, RECIPIENT_ID);

        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).findByIdAndUserId(id, RECIPIENT_ID);
    }

    @Test
    void markAllAsRead_usesRecipientScopedBulkUpdate() {
        service.markAllAsRead(RECIPIENT_ID);

        verify(notificationRepository).markAllAsRead(eq(RECIPIENT_ID), any());
    }

    @Test
    void unreadCount_isRecipientScoped() {
        when(notificationRepository.countByUserIdAndReadFalse(RECIPIENT_ID)).thenReturn(4L);

        assertThat(service.getUnreadCount(RECIPIENT_ID).unreadCount()).isEqualTo(4L);
    }

    @Test
    void deleteNotification_deletesOnlyOwnedNotification() {
        UUID id = UUID.randomUUID();
        when(notificationRepository.deleteByIdAndUserId(id, RECIPIENT_ID)).thenReturn(1L);

        service.deleteNotification(id, RECIPIENT_ID);

        verify(notificationRepository).deleteByIdAndUserId(id, RECIPIENT_ID);
    }

    @Test
    void broadcastNormalizesEveryNotificationBeforeBatchPersistence() {
        BroadcastNotificationRequest request = new BroadcastNotificationRequest(
                BroadcastAudienceType.SINGLE_USER, RECIPIENT_ID,
                NotificationType.SYSTEM, "System notice", "Body",
                Map.of("source", "admin"), null, null);
        User user = User.builder().id(RECIPIENT_ID).accountStatus(AccountStatus.ACTIVE).build();
        UserRepresentation identity = new UserRepresentation();
        identity.setId(RECIPIENT_ID);
        identity.setEnabled(true);
        RoleRepresentation userRole = new RoleRepresentation();
        userRole.setName("USER");
        when(userRepository.findById(RECIPIENT_ID)).thenReturn(Optional.of(user));
        when(keycloak.realm("rentiq").users().get(RECIPIENT_ID).toRepresentation()).thenReturn(identity);
        when(keycloak.realm("rentiq").users().get(RECIPIENT_ID).roles().realmLevel().listAll())
                .thenReturn(List.of(userRole));
        when(notificationMapper.toEntity(eq(request), any())).thenAnswer(invocation ->
                Notification.builder()
                        .userId(invocation.getArgument(1))
                        .notificationType(NotificationType.SYSTEM)
                        .title("System notice")
                        .payload(Map.of("source", "admin"))
                        .build());
        when(notificationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.broadcast(request).notificationCount()).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).allSatisfy(notification -> {
            assertThat(notification.getNotificationType()).isEqualTo(NotificationType.SYSTEM);
            assertThat(notification.getPayload()).containsEntry("eventType", "SYSTEM");
        });
    }

    @Test
    void singleUserBroadcastRejectsUnknownRecipient() {
        BroadcastNotificationRequest request = new BroadcastNotificationRequest(
                BroadcastAudienceType.SINGLE_USER, "missing-user", NotificationType.SYSTEM,
                "System notice", "Body", null, null, null);
        when(userRepository.findById("missing-user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.broadcast(request)).isInstanceOf(NotFoundException.class);
        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    void allUsersBroadcastIncludesOnlyActiveEnabledUserOrVendorAndExcludesAdmins() {
        UserRepresentation activeUser = identity("active-user", true);
        UserRepresentation suspendedUser = identity("suspended-user", true);
        UserRepresentation disabledVendor = identity("disabled-vendor", false);
        UserRepresentation activeVendor = identity("active-vendor", true);
        UserRepresentation adminWithUserRole = identity("admin-user", true);

        when(keycloak.realm("rentiq").roles().get("USER").getUserMembers(0, 250))
                .thenReturn(List.of(activeUser, suspendedUser, adminWithUserRole));
        when(keycloak.realm("rentiq").roles().get("VENDOR").getUserMembers(0, 250))
                .thenReturn(List.of(activeVendor, disabledVendor, activeUser));
        when(keycloak.realm("rentiq").roles().get("ADMIN").getUserMembers(0, 250))
                .thenReturn(List.of(adminWithUserRole));
        when(userRepository.findAllByIdInAndAccountStatus(any(), eq(AccountStatus.ACTIVE)))
                .thenReturn(List.of(
                        User.builder().id("active-user").accountStatus(AccountStatus.ACTIVE).build(),
                        User.builder().id("active-vendor").accountStatus(AccountStatus.ACTIVE).build()));
        when(notificationMapper.toEntity(any(), any())).thenAnswer(invocation ->
                Notification.builder()
                        .userId(invocation.getArgument(1))
                        .notificationType(NotificationType.SYSTEM)
                        .title("System notice")
                        .build());
        when(notificationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BroadcastNotificationRequest request = new BroadcastNotificationRequest(
                BroadcastAudienceType.ALL_USERS, null, NotificationType.SYSTEM,
                "System notice", "Body", null, null, null);
        var response = service.broadcast(request);

        assertThat(response.recipientCount()).isEqualTo(2);
        assertThat(response.notificationCount()).isEqualTo(2);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(Notification::getUserId)
                .containsExactlyInAnyOrder("active-user", "active-vendor");
    }

    @Test
    void allUsersBroadcastPersistsNotificationsInBatches() {
        List<UserRepresentation> firstRolePage = java.util.stream.IntStream.range(0, 250)
                .mapToObj(index -> identity("user-" + index, true))
                .toList();
        UserRepresentation finalUser = identity("user-250", true);
        when(keycloak.realm("rentiq").roles().get("USER").getUserMembers(0, 250))
                .thenReturn(firstRolePage);
        when(keycloak.realm("rentiq").roles().get("USER").getUserMembers(250, 250))
                .thenReturn(List.of(finalUser));
        when(keycloak.realm("rentiq").roles().get("VENDOR").getUserMembers(0, 250))
                .thenReturn(List.of());
        when(keycloak.realm("rentiq").roles().get("ADMIN").getUserMembers(0, 250))
                .thenReturn(List.of());
        when(userRepository.findAllByIdInAndAccountStatus(any(), eq(AccountStatus.ACTIVE)))
                .thenAnswer(invocation -> ((List<String>) invocation.getArgument(0)).stream()
                        .map(id -> User.builder().id(id).accountStatus(AccountStatus.ACTIVE).build())
                        .toList());
        when(notificationMapper.toEntity(any(), any())).thenAnswer(invocation ->
                Notification.builder().userId(invocation.getArgument(1))
                        .notificationType(NotificationType.SYSTEM).title("System notice").build());
        when(notificationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BroadcastNotificationRequest request = new BroadcastNotificationRequest(
                BroadcastAudienceType.ALL_USERS, null, NotificationType.SYSTEM,
                "System notice", "Body", null, null, null);

        assertThat(service.broadcast(request).notificationCount()).isEqualTo(251);
        verify(notificationRepository, org.mockito.Mockito.times(2)).saveAll(any());
    }

    private UserRepresentation identity(String id, boolean enabled) {
        UserRepresentation identity = new UserRepresentation();
        identity.setId(id);
        identity.setEnabled(enabled);
        return identity;
    }

    @Test
    void adminHistoryCapsPageSizeAtOneHundred() {
        Pageable requested = PageRequest.of(0, 500);
        when(notificationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.adminListNotifications(null, null, null, null, null, null, null, requested);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepository).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    // ---------------------------------------------------------------
    // adminListNotifications
    // ---------------------------------------------------------------

    @Test
    void adminListNotifications_delegatesToSpecificationBasedQuery() {
        Pageable pageable = mock(Pageable.class);
        when(notificationRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(Page.empty());

        service.adminListNotifications(
                RECIPIENT_ID, NotificationType.PAYMENT, false,
                NotificationReferenceType.PAYMENT, null, null, null, pageable);

        verify(notificationRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void adminListNotifications_rejectsCreatedToBeforeCreatedFrom() {
        Pageable pageable = mock(Pageable.class);
        LocalDate from = LocalDate.of(2026, 1, 10);
        LocalDate to = LocalDate.of(2026, 1, 1);

        assertThatThrownBy(() -> service.adminListNotifications(
                null, null, null, null, null, from, to, pageable))
                .isInstanceOf(InvalidOperationException.class);

        verify(notificationRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    // ---------------------------------------------------------------
    // adminGetNotification
    // ---------------------------------------------------------------

    @Test
    void adminGetNotification_returnsMappedResponse_whenFound() {
        UUID id = UUID.randomUUID();
        Notification notification = Notification.builder().id(id).userId(RECIPIENT_ID).build();
        AdminNotificationResponse response = new AdminNotificationResponse(
                id, RECIPIENT_ID, NotificationType.SYSTEM, "Title", "Body",
                null, false, null, null, null, null, NotificationType.SYSTEM, null);

        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));
        when(notificationMapper.toAdminResponse(notification)).thenReturn(response);

        AdminNotificationResponse result = service.adminGetNotification(id);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void adminGetNotification_throwsNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(notificationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.adminGetNotification(id))
                .isInstanceOf(NotFoundException.class);
    }
}
