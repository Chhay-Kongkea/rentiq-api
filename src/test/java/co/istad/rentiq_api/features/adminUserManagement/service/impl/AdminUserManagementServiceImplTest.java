package co.istad.rentiq_api.features.adminUserManagement.service.impl;

import co.istad.rentiq_api.common.config.props.KeycloakAdminClientProps;
import co.istad.rentiq_api.common.exception.InvalidStateException;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
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
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private AdminUserManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        keycloak = mock(Keycloak.class, RETURNS_DEEP_STUBS);
        KeycloakAdminClientProps props = new KeycloakAdminClientProps();
        props.setTargetRealm("rentiq");

        service = new AdminUserManagementServiceImpl(
                userRepository, userKycRepository, ownerWalletRepository, itemRepository,
                bookingRepository, reviewRepository, keycloak, props, adminAuditService);

        UserRepresentation targetRepresentation = new UserRepresentation();
        targetRepresentation.setId("user-1");
        targetRepresentation.setUsername("target");
        lenient().when(keycloak.realm("rentiq").users().get("user-1").toRepresentation())
                .thenReturn(targetRepresentation);
        lenient().when(keycloak.realm("rentiq").users().get("user-1").roles().realmLevel().listAll())
                .thenReturn(List.of());
    }

    @Test
    void suspendUser_movesActiveUserToSuspended_andRecordsAudit() {
        User user = User.builder().id("user-1").accountStatus(AccountStatus.ACTIVE).build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.suspendUser("user-1", "Repeated policy violations", "admin-1");

        verify(adminAuditService).record(
                AdminAuditAction.USER_SUSPENDED,
                AdminAuditTargetType.USER,
                "user-1",
                Map.of("status", "ACTIVE"),
                Map.of("status", "SUSPENDED"),
                "Repeated policy violations");
    }

    @Test
    void suspendUser_rejectsAlreadySuspendedUser_andDoesNotRecordAudit() {
        User user = User.builder().id("user-1").accountStatus(AccountStatus.SUSPENDED).build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.suspendUser("user-1", "reason", "admin-1"))
                .isInstanceOf(InvalidStateException.class);

        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
    }

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
}
