package co.istad.rentiq_api.features.userProfile.service;


import co.istad.rentiq_api.features.userProfile.dto.request.AddressRequest;
import co.istad.rentiq_api.features.userProfile.dto.request.NotificationPreferencesRequest;
import co.istad.rentiq_api.features.userProfile.dto.request.UpdateProfileRequest;
import co.istad.rentiq_api.features.userProfile.dto.response.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface UserProfileService {

    UserProfileResponse getMyProfile(String userId);

    UserProfileResponse updateMyProfile(String userId, UpdateProfileRequest request);

    PublicUserProfileResponse getPublicProfile(String targetUserId);

    AvatarResponse uploadAvatar(String userId, MultipartFile file);

    void deleteAvatar(String userId);

    List<AddressResponse> listAddresses(String userId);

    AddressResponse getAddress(String userId, UUID addressId);

    AddressResponse addAddress(String userId, AddressRequest request);

    AddressResponse updateAddress(String userId, UUID addressId, AddressRequest request);

    void deleteAddress(String userId, UUID addressId);

    AddressResponse setDefaultAddress(String userId, UUID addressId);

    NotificationPreferencesResponse getNotificationPreferences(String userId);

    NotificationPreferencesResponse updateNotificationPreferences(String userId, NotificationPreferencesRequest request);

    // Called from AuthServiceImpl.register() to bootstrap the local profile row.
    void createProfileIfAbsent(String userId);
}