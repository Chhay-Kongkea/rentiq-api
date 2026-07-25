package co.istad.rentiq_api.features.userProfile.controller;



import co.istad.rentiq_api.features.userProfile.dto.request.*;
import co.istad.rentiq_api.features.userProfile.dto.response.*;
import co.istad.rentiq_api.features.userProfile.service.UserProfileService;
import co.istad.rentiq_api.security.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public UserProfileResponse getMyProfile() {
        return userProfileService.getMyProfile(AuthUtils.extractUserId());
    }

    @PatchMapping("/me")
    public UserProfileResponse updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return userProfileService.updateMyProfile(AuthUtils.extractUserId(), request);
    }

    @GetMapping("/{id}")
    public PublicUserProfileResponse getPublicProfile(@PathVariable String id) {
        return userProfileService.getPublicProfile(id);
    }


    @PostMapping(value = "/me/avatar", consumes = "multipart/form-data")
    public AvatarResponse uploadAvatar(@RequestParam("file") MultipartFile file) {
        return userProfileService.uploadAvatar(AuthUtils.extractUserId(), file);
    }

    @DeleteMapping("/me/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAvatar() {
        userProfileService.deleteAvatar(AuthUtils.extractUserId());
    }



    @GetMapping("/me/addresses")
    public List<AddressResponse> listAddresses() {
        return userProfileService.listAddresses(AuthUtils.extractUserId());
    }

    @PostMapping("/me/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse addAddress(@Valid @RequestBody AddressRequest request) {
        return userProfileService.addAddress(AuthUtils.extractUserId(), request);
    }

    @GetMapping("/me/addresses/{addressId}")
    public AddressResponse getAddress(@PathVariable UUID addressId) {
        return userProfileService.getAddress(AuthUtils.extractUserId(), addressId);
    }

    @PatchMapping("/me/addresses/{addressId}")
    public AddressResponse updateAddress(@PathVariable UUID addressId,
                                         @Valid @RequestBody AddressRequest request) {
        return userProfileService.updateAddress(AuthUtils.extractUserId(), addressId, request);
    }

    @DeleteMapping("/me/addresses/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(@PathVariable UUID addressId) {
        userProfileService.deleteAddress(AuthUtils.extractUserId(), addressId);
    }

    @PatchMapping("/me/addresses/{addressId}/default")
    public AddressResponse setDefaultAddress(@PathVariable UUID addressId) {
        return userProfileService.setDefaultAddress(AuthUtils.extractUserId(), addressId);
    }



    @GetMapping("/me/notification-preferences")
    public NotificationPreferencesResponse getNotificationPreferences() {
        return userProfileService.getNotificationPreferences(AuthUtils.extractUserId());
    }

    @PatchMapping("/me/notification-preferences")
    public NotificationPreferencesResponse updateNotificationPreferences(
            @Valid @RequestBody NotificationPreferencesRequest request) {
        return userProfileService.updateNotificationPreferences(AuthUtils.extractUserId(), request);
    }
}