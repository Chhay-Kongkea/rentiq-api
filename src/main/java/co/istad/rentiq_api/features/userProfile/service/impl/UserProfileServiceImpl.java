package co.istad.rentiq_api.features.userProfile.service.impl;




import co.istad.rentiq_api.common.config.props.KeycloakAdminClientProps;
import co.istad.rentiq_api.features.notification.entity.NotificationPreference;
import co.istad.rentiq_api.features.notification.repository.NotificationPreferenceRepository;
import co.istad.rentiq_api.features.userProfile.dto.request.AddressRequest;
import co.istad.rentiq_api.features.userProfile.dto.request.NotificationPreferencesRequest;
import co.istad.rentiq_api.features.userProfile.dto.request.UpdateProfileRequest;
import co.istad.rentiq_api.features.userProfile.dto.response.*;
import co.istad.rentiq_api.features.userProfile.entity.User;
import co.istad.rentiq_api.features.userProfile.entity.UserAddress;
import co.istad.rentiq_api.features.userProfile.enums.AccountStatus;
import co.istad.rentiq_api.features.userProfile.exception.UserProfileException;
import co.istad.rentiq_api.features.userProfile.exception.UserProfileNotFoundException;
import co.istad.rentiq_api.features.userProfile.exception.AddressNotFoundException;
import co.istad.rentiq_api.features.userProfile.mapper.UserProfileMapper;
import co.istad.rentiq_api.features.userProfile.repository.UserAddressRepository;
import co.istad.rentiq_api.features.userProfile.repository.UserRepository;
import co.istad.rentiq_api.features.userProfile.service.AvatarStorageService;
import co.istad.rentiq_api.features.userProfile.service.UserProfileService;
import co.istad.rentiq_api.features.userProfile.util.GeoUtils;
import co.istad.rentiq_api.features.localization.enums.SupportedLocale;
import co.istad.rentiq_api.security.AuthUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final UserAddressRepository addressRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final AvatarStorageService avatarStorageService;
    private final Keycloak keycloak;
    private final KeycloakAdminClientProps props;
    private final UserProfileMapper userProfileMapper;

    // ---------------------------------------------------------------
    // PROFILE
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public UserProfileResponse getMyProfile(String userId) {

        User profile = getOrCreateProfile(userId);

        return UserProfileResponse.builder()
                .id(userId)
                .username(AuthUtils.extractUsername())
                .email(AuthUtils.extractEmail())
                .firstName(AuthUtils.extractFirstName())
                .lastName(AuthUtils.extractLastName())
                .avatarUrl(profile.getAvatarUrl())
                .locale(profile.getLocale())
                .accountStatus(profile.getAccountStatus().toString())
                .memberSince(profile.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public UserProfileResponse updateMyProfile(String userId, UpdateProfileRequest request) {

        User profile = getOrCreateProfile(userId);

        if (request.locale() != null && !request.locale().isBlank()) {
            // Must resolve through the same SupportedLocale the Localization API actually
            // serves (en/km) — a persisted locale that GET /api/v1/locales/{code}/strings
            // can't serve would be a stored dead value (backend audit BUS-003).
            SupportedLocale locale = SupportedLocale.fromCode(request.locale())
                    .orElseThrow(() -> new UserProfileException(
                            HttpStatus.BAD_REQUEST, "Unsupported locale: " + request.locale()));
            profile.setLocale(locale.getCode());
        }

        userRepository.save(profile);
        log.info("Profile updated for user {}", userId);

        return getMyProfile(userId);
    }

    @Override
    public PublicUserProfileResponse getPublicProfile(String targetUserId) {

        UserRepresentation kcUser;
        try {
            kcUser = keycloak.realm(props.getTargetRealm())
                    .users()
                    .get(targetUserId)
                    .toRepresentation();
        } catch (jakarta.ws.rs.NotFoundException e) {
            throw new UserProfileNotFoundException(targetUserId);
        } catch (RuntimeException e) {
            log.error("Failed to fetch Keycloak profile for {}", targetUserId, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch user profile");
        }

        User profile = userRepository.findById(targetUserId).orElse(null);

        return PublicUserProfileResponse.builder()
                .id(targetUserId)
                .username(kcUser.getUsername())
                .firstName(kcUser.getFirstName())
                .lastName(kcUser.getLastName())
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .memberSince(profile != null ? profile.getCreatedAt() : null)
                .build();
    }

    // ---------------------------------------------------------------
    // AVATAR
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public AvatarResponse uploadAvatar(String userId, MultipartFile file) {

        User profile = getOrCreateProfile(userId);

        String url = avatarStorageService.upload(userId, file);

        profile.setAvatarUrl(url);
        userRepository.save(profile);

        return AvatarResponse.builder().avatarUrl(url).build();
    }

    @Override
    @Transactional
    public void deleteAvatar(String userId) {

        User profile = getOrCreateProfile(userId);

        if (profile.getAvatarUrl() == null) {
            return; // idempotent — nothing to delete
        }

        avatarStorageService.delete(userId);
        profile.setAvatarUrl(null);
        userRepository.save(profile);
    }

    // ---------------------------------------------------------------
    // ADDRESSES
    // ---------------------------------------------------------------

    @Override
    public List<AddressResponse> listAddresses(String userId) {
        return addressRepository.findAllByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream()
                .map(userProfileMapper::toResponse)
                .toList();
    }

    @Override
    public AddressResponse getAddress(String userId, UUID addressId) {
        UserAddress address = findOwnedAddress(userId, addressId);
        return userProfileMapper.toResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse addAddress(String userId, AddressRequest request) {

        boolean isFirstAddress = addressRepository.countByUserId(userId) == 0;

        UserAddress address = UserAddress.builder()
                .userId(userId)
                .addressLine(request.addressLine())
                .city(request.city())
                .country(request.country() != null ? request.country() : "KHM")
                .location(GeoUtils.toPoint(request.latitude(), request.longitude()))
                // First address is always default; otherwise honor the request flag.
                .isDefault(isFirstAddress || Boolean.TRUE.equals(request.isDefault()))
                .build();

        if (address.isDefault()) {
            clearExistingDefault(userId);
        }

        address = addressRepository.save(address);
        log.info("Address added for user {}: {}", userId, address.getId());

        return userProfileMapper.toResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(String userId, UUID addressId, AddressRequest request) {

        UserAddress address = findOwnedAddress(userId, addressId);

        address.setAddressLine(request.addressLine());
        address.setCity(request.city());
        if (request.country() != null) {
            address.setCountry(request.country());
        }
        if (request.latitude() != null && request.longitude() != null) {
            address.setLocation(GeoUtils.toPoint(request.latitude(), request.longitude()));
        }

        if (Boolean.TRUE.equals(request.isDefault()) && !address.isDefault()) {
            clearExistingDefault(userId);
            address.setDefault(true);
        }

        address = addressRepository.save(address);
        log.info("Address updated for user {}: {}", userId, addressId);

        return userProfileMapper.toResponse(address);
    }

    @Override
    @Transactional
    public void deleteAddress(String userId, UUID addressId) {

        UserAddress address = findOwnedAddress(userId, addressId);
        boolean wasDefault = address.isDefault();

        addressRepository.delete(address);
        log.info("Address deleted for user {}: {}", userId, addressId);

        // Promote another address to default so the user always has one, if any remain.
        if (wasDefault) {
            addressRepository.findAllByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                    .stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setDefault(true);
                        addressRepository.save(next);
                    });
        }
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(String userId, UUID addressId) {

        UserAddress address = findOwnedAddress(userId, addressId);

        if (!address.isDefault()) {
            clearExistingDefault(userId);
            address.setDefault(true);
            address = addressRepository.save(address);
        }

        return userProfileMapper.toResponse(address);
    }

    // ---------------------------------------------------------------
    // NOTIFICATION PREFERENCES
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public NotificationPreferencesResponse getNotificationPreferences(String userId) {
        return userProfileMapper.toPreferencesResponse(getOrCreatePreferences(userId));
    }

    @Override
    @Transactional
    public NotificationPreferencesResponse updateNotificationPreferences(
            String userId, NotificationPreferencesRequest request) {

        NotificationPreference prefs = getOrCreatePreferences(userId);

        if (request.bookingNotifications() != null) prefs.setBookingNotifications(request.bookingNotifications());
        if (request.paymentNotifications() != null) prefs.setPaymentNotifications(request.paymentNotifications());
        if (request.marketingNotifications() != null) prefs.setMarketingNotifications(request.marketingNotifications());
        if (request.emailNotifications() != null) prefs.setEmailNotifications(request.emailNotifications());
        if (request.pushNotifications() != null) prefs.setPushNotifications(request.pushNotifications());

        notificationPreferenceRepository.save(prefs);
        log.info("Notification preferences updated for user {}", userId);

        return userProfileMapper.toPreferencesResponse(prefs);
    }

    // ---------------------------------------------------------------
    // BOOTSTRAP
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public void createProfileIfAbsent(String userId) {
        getOrCreateProfile(userId);
        getOrCreatePreferences(userId);
    }

    // ---------------------------------------------------------------
    // PRIVATE HELPERS
    // ---------------------------------------------------------------

    private User getOrCreateProfile(String userId) {
        return userRepository.findById(userId)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .id(userId)
                                .locale("en")
                                .accountStatus(AccountStatus.ACTIVE)
                                .build()
                ));
    }

    private NotificationPreference getOrCreatePreferences(String userId) {
        return notificationPreferenceRepository.findById(userId)
                .orElseGet(() -> notificationPreferenceRepository.save(
                        NotificationPreference.builder().userId(userId).build()
                ));
    }

    private UserAddress findOwnedAddress(String userId, UUID addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(AddressNotFoundException::new);
    }

    private void clearExistingDefault(String userId) {
        addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(current -> {
                    current.setDefault(false);
                    addressRepository.save(current);
                });
    }


}