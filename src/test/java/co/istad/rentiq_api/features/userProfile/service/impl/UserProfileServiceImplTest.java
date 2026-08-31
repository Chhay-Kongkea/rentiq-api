package co.istad.rentiq_api.features.userProfile.service.impl;

import co.istad.rentiq_api.common.config.props.KeycloakAdminClientProps;
import co.istad.rentiq_api.features.notification.repository.NotificationPreferenceRepository;
import co.istad.rentiq_api.features.userProfile.dto.request.UpdateProfileRequest;
import co.istad.rentiq_api.features.userProfile.entity.User;
import co.istad.rentiq_api.features.userProfile.enums.AccountStatus;
import co.istad.rentiq_api.features.userProfile.exception.UserProfileException;
import co.istad.rentiq_api.features.userProfile.mapper.UserProfileMapper;
import co.istad.rentiq_api.features.userProfile.repository.UserAddressRepository;
import co.istad.rentiq_api.features.userProfile.repository.UserRepository;
import co.istad.rentiq_api.features.userProfile.service.AvatarStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Backend audit BUS-003 — {@code User.locale} must validate through {@link
 * co.istad.rentiq_api.features.localization.enums.SupportedLocale} (the same closed set the
 * Localization API actually serves), case-insensitively, and reject anything else cleanly
 * instead of persisting a dead value.
 */
@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserAddressRepository addressRepository;
    @Mock private NotificationPreferenceRepository notificationPreferenceRepository;
    @Mock private AvatarStorageService avatarStorageService;
    @Mock private Keycloak keycloak;
    @Mock private KeycloakAdminClientProps props;
    @Mock private UserProfileMapper userProfileMapper;

    private UserProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserProfileServiceImpl(
                userRepository, addressRepository, notificationPreferenceRepository,
                avatarStorageService, keycloak, props, userProfileMapper);
        // getMyProfile() (called at the end of updateMyProfile) reads the JWT via AuthUtils,
        // which requires an authenticated SecurityContext this unit test never sets up — the
        // locale-validation branch under test always runs (and, on rejection, throws) before
        // that point is ever reached, so clearing the context here just documents the boundary.
        SecurityContextHolder.clearContext();
    }

    private User existingProfile() {
        return User.builder().id("user-1").locale("en").accountStatus(AccountStatus.ACTIVE).build();
    }

    @Test
    void updateMyProfile_acceptsSupportedLocale_caseInsensitive_andNormalizesToLowercase() {
        User profile = existingProfile();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(profile));

        // The AuthUtils-backed getMyProfile() tail call has no SecurityContext in this test, so
        // it throws — but the locale mutation and repository save happen first and are what
        // this test verifies.
        assertThatThrownBy(() -> service.updateMyProfile("user-1", new UpdateProfileRequest("KM")))
                .isInstanceOf(ResponseStatusException.class);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getLocale()).isEqualTo("km");
    }

    @Test
    void updateMyProfile_rejectsUnsupportedLocale_withoutSaving() {
        User profile = existingProfile();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.updateMyProfile("user-1", new UpdateProfileRequest("fr")))
                .isInstanceOf(UserProfileException.class)
                .hasMessageContaining("fr");

        verify(userRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateMyProfile_blankLocale_leavesExistingLocaleUntouched() {
        User profile = existingProfile();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.updateMyProfile("user-1", new UpdateProfileRequest(null)))
                .isInstanceOf(ResponseStatusException.class);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getLocale()).isEqualTo("en");
    }
}
