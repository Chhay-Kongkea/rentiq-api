package co.istad.rentiq_api.features.adminUserManagement.service.impl;

import co.istad.rentiq_api.common.config.props.KeycloakAdminClientProps;
import co.istad.rentiq_api.common.exception.ForbiddenException;
import co.istad.rentiq_api.common.exception.InvalidStateException;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
import co.istad.rentiq_api.features.adminUserManagement.dto.response.AdminUserStatusResponse;
import co.istad.rentiq_api.features.auth.exception.KeycloakOperationException;
import co.istad.rentiq_api.features.bookings.repository.BookingRepository;
import co.istad.rentiq_api.features.item.repository.ItemRepository;
import co.istad.rentiq_api.features.kyc.repository.UserKycRepository;
import co.istad.rentiq_api.features.review.repository.ReviewRepository;
import co.istad.rentiq_api.features.userProfile.entity.User;
import co.istad.rentiq_api.features.userProfile.enums.AccountStatus;
import co.istad.rentiq_api.features.userProfile.repository.UserRepository;
import co.istad.rentiq_api.features.wallet.repository.OwnerWalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Backend audit P0-2 — this is the single canonical vendor/user moderation implementation.
 * VendorPerformanceServiceImpl (the old /admin/vendors route) now delegates suspend/ban/reinstate
 * here instead of maintaining its own divergent Keycloak-sync logic. Covers: status transitions,
 * Keycloak enabled-state sync (the actual defense against a banned account re-authenticating),
 * repeated/invalid transitions, audit recording, and failure consistency when Keycloak sync fails.
 */
@ExtendWith(MockitoExtension.class)
class AdminUserManagementServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserKycRepository userKycRepository;
    @Mock private OwnerWalletRepository ownerWalletRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private AdminAuditService adminAuditService;

    private Keycloak keycloak;
    private UserResource targetUserResource;
    private AdminUserManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        keycloak = mock(Keycloak.class, RETURNS_DEEP_STUBS);
        KeycloakAdminClientProps props = new KeycloakAdminClientProps();
        props.setTargetRealm("rentiq");

        service = new AdminUserManagementServiceImpl(
                userRepository, userKycRepository, ownerWalletRepository, itemRepository,
                bookingRepository, reviewRepository, keycloak, props, adminAuditService);

        targetUserResource = keycloak.realm("rentiq").users().get("user-1");

        // A fresh UserRepresentation per call — syncKeycloakEnabledState mutates the object it
        // reads before calling update(), so a shared/reused instance would make every recorded
        // update() invocation appear to carry the *latest* enabled value instead of the one
        // actually sent at that call, which would hide the ban-then-revert ordering below.
        lenient().when(targetUserResource.toRepresentation()).thenAnswer(invocation -> {
            UserRepresentation representation = new UserRepresentation();
            representation.setId("user-1");
            representation.setUsername("target");
            representation.setEnabled(true);
            return representation;
        });
        lenient().when(targetUserResource.roles().realmLevel().listAll()).thenReturn(List.of());
    }

    private User activeUser() {
        return User.builder().id("user-1").accountStatus(AccountStatus.ACTIVE).build();
    }

    // ================================================================
    // SUSPEND
    // ================================================================

    @Test
    void suspendUser_disablesKeycloakBeforeLocalCommit_movesActiveUserToSuspended_andRecordsAudit() {
        User user = activeUser();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserStatusResponse response = service.suspendUser("user-1", "Repeated policy violations", "admin-1");

        assertThat(response.previousStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(response.accountStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);

        verify(targetUserResource).update(argThatEnabled(false));
        verify(targetUserResource).logout();
        verify(adminAuditService).record(
                AdminAuditAction.USER_SUSPENDED,
                AdminAuditTargetType.USER,
                "user-1",
                java.util.Map.of("status", "ACTIVE"),
                java.util.Map.of("status", "SUSPENDED"),
                "Repeated policy violations");
    }

    @Test
    void suspendUser_rejectsAlreadySuspendedUser_andDoesNotRecordAudit() {
        User user = User.builder().id("user-1").accountStatus(AccountStatus.SUSPENDED).build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.suspendUser("user-1", "reason", "admin-1"))
                .isInstanceOf(InvalidStateException.class);

        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
        verify(targetUserResource, never()).update(any());
    }

    // ================================================================
    // BAN
    // ================================================================

    @Test
    void banUser_disablesKeycloak_movesUserToBanned_andRecordsAudit() {
        User user = activeUser();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.banUser("user-1", "fraud", "admin-1");

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.BANNED);
        verify(targetUserResource).update(argThatEnabled(false));
        verify(targetUserResource).logout();
        verify(adminAuditService).record(
                AdminAuditAction.USER_BANNED,
                AdminAuditTargetType.USER,
                "user-1",
                java.util.Map.of("status", "ACTIVE"),
                java.util.Map.of("status", "BANNED"),
                "fraud");
    }

    @Test
    void banUser_rejectsAlreadyBannedUser_repeatedOperation() {
        User user = User.builder().id("user-1").accountStatus(AccountStatus.BANNED).build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.banUser("user-1", "reason", "admin-1"))
                .isInstanceOf(InvalidStateException.class);

        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void banUser_canBanASuspendedUser_notOnlyAnActiveOne() {
        User user = User.builder().id("user-1").accountStatus(AccountStatus.SUSPENDED).build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserStatusResponse response = service.banUser("user-1", "escalated", "admin-1");

        assertThat(response.accountStatus()).isEqualTo(AccountStatus.BANNED);
    }

    // ================================================================
    // REINSTATE
    // ================================================================

    @Test
    void reinstateUser_reEnablesKeycloak_movesUserToActive_andRecordsAudit() {
        User user = User.builder().id("user-1").accountStatus(AccountStatus.BANNED).build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.reinstateUser("user-1", "appeal approved", "admin-1");

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(targetUserResource).update(argThatEnabled(true));
        // Reinstatement never revokes sessions — only suspend/ban do.
        verify(targetUserResource, never()).logout();
        verify(adminAuditService).record(
                AdminAuditAction.USER_REINSTATED,
                AdminAuditTargetType.USER,
                "user-1",
                java.util.Map.of("status", "BANNED"),
                java.util.Map.of("status", "ACTIVE"),
                "appeal approved");
    }

    @Test
    void reinstateUser_rejectsAlreadyActiveUser_invalidTransition() {
        User user = activeUser();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.reinstateUser("user-1", "reason", "admin-1"))
                .isInstanceOf(InvalidStateException.class);

        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
    }

    // ================================================================
    // GUARDS
    // ================================================================

    @Test
    void suspendUser_rejectsSelfModeration() {
        User admin = User.builder().id("admin-1").accountStatus(AccountStatus.ACTIVE).build();
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.suspendUser("admin-1", "reason", "admin-1"))
                .isInstanceOf(ForbiddenException.class);

        verify(userRepository, never()).saveAndFlush(any());
    }

    // ================================================================
    // KEYCLOAK FAILURE — must not leave a partially-completed moderation state
    // ================================================================

    @Test
    void suspendUser_whenKeycloakSyncFails_leavesLocalStatusUntouched_andRecordsNoAudit() {
        User user = activeUser();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(targetUserResource.toRepresentation()).thenThrow(new RuntimeException("Keycloak unreachable"));

        assertThatThrownBy(() -> service.suspendUser("user-1", "reason", "admin-1"))
                .isInstanceOf(KeycloakOperationException.class);

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(userRepository, never()).saveAndFlush(any());
        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void banUser_whenLocalCommitFailsAfterKeycloakDisabled_revertsKeycloakBackToEnabled() {
        User user = activeUser();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(any())).thenThrow(new RuntimeException("DB constraint violation"));

        assertThatThrownBy(() -> service.banUser("user-1", "reason", "admin-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB constraint violation");

        // First call disables (ban), second call is the compensating revert back to enabled.
        verify(targetUserResource, times(2)).update(any());
        var inOrder = inOrder(targetUserResource);
        inOrder.verify(targetUserResource).update(argThatEnabled(false));
        inOrder.verify(targetUserResource).update(argThatEnabled(true));
        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
    }

    // ================================================================
    // VENDOR MODERATION — same canonical logic, VENDOR audit target/action
    // ================================================================

    @Test
    void suspendVendor_usesVendorAuditTargetAndAction() {
        User user = activeUser();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.suspendVendor("user-1", "policy violation", "admin-1");

        verify(targetUserResource).update(argThatEnabled(false));
        verify(adminAuditService).record(
                AdminAuditAction.VENDOR_SUSPENDED,
                AdminAuditTargetType.VENDOR,
                "user-1",
                java.util.Map.of("status", "ACTIVE"),
                java.util.Map.of("status", "SUSPENDED"),
                "policy violation");
    }

    @Test
    void banVendor_disablesKeycloak_soABannedVendorCannotReAuthenticate() {
        User user = activeUser();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.banVendor("user-1", "fraud", "admin-1");

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.BANNED);
        verify(targetUserResource).update(argThatEnabled(false));
        verify(targetUserResource).logout();
        verify(adminAuditService).record(
                AdminAuditAction.VENDOR_BANNED,
                AdminAuditTargetType.VENDOR,
                "user-1",
                java.util.Map.of("status", "ACTIVE"),
                java.util.Map.of("status", "BANNED"),
                "fraud");
    }

    @Test
    void banVendor_rejectsRepeatedBan() {
        User user = User.builder().id("user-1").accountStatus(AccountStatus.BANNED).build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.banVendor("user-1", "reason", "admin-1"))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    void reinstateVendor_reEnablesKeycloak_andRecordsVendorAudit() {
        User user = User.builder().id("user-1").accountStatus(AccountStatus.BANNED).build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.reinstateVendor("user-1", "appeal approved", "admin-1");

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(targetUserResource).update(argThatEnabled(true));
        verify(adminAuditService).record(
                AdminAuditAction.VENDOR_REINSTATED,
                AdminAuditTargetType.VENDOR,
                "user-1",
                java.util.Map.of("status", "BANNED"),
                java.util.Map.of("status", "ACTIVE"),
                "appeal approved");
    }

    @Test
    void reinstateVendor_whenKeycloakSyncFails_leavesLocalStatusUntouched() {
        User user = User.builder().id("user-1").accountStatus(AccountStatus.BANNED).build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        doThrow(new RuntimeException("Keycloak unreachable")).when(targetUserResource).update(any());
        when(targetUserResource.toRepresentation()).thenReturn(new UserRepresentation());

        assertThatThrownBy(() -> service.reinstateVendor("user-1", "reason", "admin-1"))
                .isInstanceOf(KeycloakOperationException.class);

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.BANNED);
        verify(userRepository, never()).saveAndFlush(any());
        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
    }

    // ================================================================
    // (pre-existing) listing behavior — unaffected by this change
    // ================================================================

    @Test
    void listUsers_searchesKeycloakAndReturnsOnlyLocalProfilesWithBoundedPagination() {
        User user = User.builder().id("user-1").accountStatus(AccountStatus.ACTIVE).build();
        UserRepresentation identity = new UserRepresentation();
        identity.setId("user-1");
        identity.setUsername("target");
        identity.setEmail("target@example.com");
        when(keycloak.realm("rentiq").users().search("target", 0, 100))
                .thenReturn(List.of(identity));
        when(keycloak.realm("rentiq").users().count("target")).thenReturn(1);
        when(userRepository.findAllById(List.of("user-1"))).thenReturn(List.of(user));

        Page<?> result = service.listUsers(" target ", PageRequest.of(0, 500));

        assertThat(result.getSize()).isEqualTo(100);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
    }

    private static UserRepresentation argThatEnabled(boolean expected) {
        return org.mockito.ArgumentMatchers.argThat(rep -> rep != null && rep.isEnabled() == expected);
    }
}
