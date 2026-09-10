package co.istad.rentiq_api.features.adminUserManagement.service.impl;

import co.istad.rentiq_api.common.config.props.KeycloakAdminClientProps;
import co.istad.rentiq_api.common.exception.ForbiddenException;
import co.istad.rentiq_api.common.exception.InvalidStateException;
import co.istad.rentiq_api.common.exception.NotFoundException;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
import co.istad.rentiq_api.features.adminUserManagement.dto.response.AdminUserResponse;
import co.istad.rentiq_api.features.adminUserManagement.dto.response.AdminUserStatusResponse;
import co.istad.rentiq_api.features.adminUserManagement.dto.response.AdminVendorResponse;
import co.istad.rentiq_api.features.adminUserManagement.service.AdminUserManagementService;
import co.istad.rentiq_api.features.auth.RoleEnum;
import co.istad.rentiq_api.features.auth.exception.KeycloakOperationException;
import co.istad.rentiq_api.features.bookings.repository.BookingRepository;
import co.istad.rentiq_api.features.item.repository.ItemRepository;
import co.istad.rentiq_api.features.kyc.repository.UserKycRepository;
import co.istad.rentiq_api.features.review.repository.ReviewRepository;
import co.istad.rentiq_api.features.userProfile.entity.User;
import co.istad.rentiq_api.features.userProfile.enums.AccountStatus;
import co.istad.rentiq_api.features.userProfile.repository.UserRepository;
import co.istad.rentiq_api.features.wallet.entity.OwnerWallet;
import co.istad.rentiq_api.features.wallet.repository.OwnerWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AdminUserManagementServiceImpl implements AdminUserManagementService {

    private static final String KYC_NOT_SUBMITTED = "NOT_SUBMITTED";
    private static final int MAX_USER_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final UserKycRepository userKycRepository;
    private final OwnerWalletRepository ownerWalletRepository;
    private final ItemRepository itemRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final Keycloak keycloak;
    private final KeycloakAdminClientProps keycloakProps;
    private final AdminAuditService adminAuditService;

    @Override
    public Page<AdminUserResponse> listUsers(Pageable pageable) {
        Pageable bounded = boundedPageable(pageable);
        return userRepository.findAll(bounded)
                .map(this::toAdminUserResponse);
    }

    @Override
    public Page<AdminUserResponse> listUsers(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return listUsers(pageable);
        }

        Pageable bounded = boundedPageable(pageable);
        String query = search.trim();
        try {
            List<UserRepresentation> identities = keycloak.realm(keycloakProps.getTargetRealm())
                    .users().search(query, Math.toIntExact(bounded.getOffset()), bounded.getPageSize());
            Map<String, User> localUsers = userRepository.findAllById(
                            identities.stream().map(UserRepresentation::getId).toList())
                    .stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));

            List<AdminUserResponse> content = identities.stream()
                    .map(identity -> localUsers.get(identity.getId()))
                    .filter(java.util.Objects::nonNull)
                    .map(this::toAdminUserResponse)
                    .toList();
            long total = keycloak.realm(keycloakProps.getTargetRealm()).users().count(query);
            return new PageImpl<>(content, bounded, total);
        } catch (RuntimeException exception) {
            throw new KeycloakOperationException("Failed to search identity users", exception);
        }
    }

    @Override
    public AdminUserResponse getUser(String userId) {
        User user = requireUser(userId);
        return toAdminUserResponse(user);
    }

    @Override
    @Transactional
    public AdminUserStatusResponse suspendUser(String userId, String reason, String adminId) {
        User user = requireModeratableUser(userId, adminId);
        requireActive(user, "User", "Only an active user can be suspended");

        return changeStatus(user, AccountStatus.SUSPENDED, reason,
                AdminAuditTargetType.USER, AdminAuditAction.USER_SUSPENDED);
    }

    @Override
    @Transactional
    public AdminUserStatusResponse banUser(String userId, String reason, String adminId) {
        User user = requireModeratableUser(userId, adminId);
        requireNotAlready(user, AccountStatus.BANNED, "User", "User is already banned");

        return changeStatus(user, AccountStatus.BANNED, reason,
                AdminAuditTargetType.USER, AdminAuditAction.USER_BANNED);
    }

    @Override
    @Transactional
    public AdminUserStatusResponse reinstateUser(String userId, String reason, String adminId) {
        User user = requireModeratableUser(userId, adminId);
        requireNotAlready(user, AccountStatus.ACTIVE, "User", "User is already active");

        return changeStatus(user, AccountStatus.ACTIVE, reason,
                AdminAuditTargetType.USER, AdminAuditAction.USER_REINSTATED);
    }

    // ================================================================
    // VENDOR MODERATION
    //
    // Backend audit P0-2 — vendor moderation used to live in a second, divergent
    // implementation (VendorPerformanceServiceImpl) that never disabled the Keycloak
    // account, only logged out existing sessions. A banned vendor could simply log back
    // in. This is now the single canonical moderation implementation for both the
    // /admin/users and /admin/vendors routes; only the audit target/action differs.
    // ================================================================

    @Override
    @Transactional
    public AdminUserStatusResponse suspendVendor(String userId, String reason, String adminId) {
        User user = requireModeratableUser(userId, adminId);
        requireActive(user, "Vendor", "Only an active vendor can be suspended");

        return changeStatus(user, AccountStatus.SUSPENDED, reason,
                AdminAuditTargetType.VENDOR, AdminAuditAction.VENDOR_SUSPENDED);
    }

    @Override
    @Transactional
    public AdminUserStatusResponse banVendor(String userId, String reason, String adminId) {
        User user = requireModeratableUser(userId, adminId);
        requireNotAlready(user, AccountStatus.BANNED, "Vendor", "Vendor is already banned");

        return changeStatus(user, AccountStatus.BANNED, reason,
                AdminAuditTargetType.VENDOR, AdminAuditAction.VENDOR_BANNED);
    }

    @Override
    @Transactional
    public AdminUserStatusResponse reinstateVendor(String userId, String reason, String adminId) {
        User user = requireModeratableUser(userId, adminId);
        requireNotAlready(user, AccountStatus.ACTIVE, "Vendor", "Vendor is already active");

        return changeStatus(user, AccountStatus.ACTIVE, reason,
                AdminAuditTargetType.VENDOR, AdminAuditAction.VENDOR_REINSTATED);
    }

    private void requireActive(User user, String resourceName, String message) {
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new InvalidStateException(resourceName, user.getAccountStatus(), message);
        }
    }

    private void requireNotAlready(User user, AccountStatus status, String resourceName, String message) {
        if (user.getAccountStatus() == status) {
            throw new InvalidStateException(resourceName, user.getAccountStatus(), message);
        }
    }

    @Override
    public Page<AdminVendorResponse> listVendors(Pageable pageable) {
        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "createdAt");

        List<VendorSeed> vendors = userRepository.findAll(sort).stream()
                .map(this::toVendorSeedIfVendor)
                .filter(seed -> seed != null)
                .toList();

        int start = Math.toIntExact(Math.min(pageable.getOffset(), vendors.size()));
        int end = Math.min(start + pageable.getPageSize(), vendors.size());

        List<AdminVendorResponse> content = vendors.subList(start, end).stream()
                .map(this::toAdminVendorResponse)
                .toList();

        return new PageImpl<>(content, pageable, vendors.size());
    }

    @Override
    public AdminVendorResponse getVendor(String userId) {
        User user = requireUser(userId);
        IdentitySnapshot identity = getIdentitySnapshot(userId);

        if (!identity.roles().contains(RoleEnum.VENDOR.name())) {
            throw new NotFoundException("Vendor", userId);
        }

        return toAdminVendorResponse(new VendorSeed(user, identity));
    }

    private AdminUserStatusResponse changeStatus(
            User user,
            AccountStatus newStatus,
            String reason,
            AdminAuditTargetType targetType,
            AdminAuditAction action
    ) {
        AccountStatus previousStatus = user.getAccountStatus();
        boolean targetEnabled = newStatus == AccountStatus.ACTIVE;

        // Keycloak is synchronized FIRST. If this fails, execution stops here: no local
        // status change, no audit entry — never a misleading partially-completed moderation
        // state (backend audit P0-2).
        syncKeycloakEnabledState(user.getId(), targetEnabled);

        try {
            user.setAccountStatus(newStatus);
            user = userRepository.saveAndFlush(user);

            if (newStatus != AccountStatus.ACTIVE) {
                revokeKeycloakSessionsQuietly(user.getId());
            }

            adminAuditService.record(
                    action,
                    targetType,
                    user.getId(),
                    Map.of("status", previousStatus.name()),
                    Map.of("status", newStatus.name()),
                    reason);
        } catch (RuntimeException e) {
            // Keycloak already flipped but the local commit/audit failed — revert Keycloak
            // best-effort instead of leaving the identity provider silently out of sync with
            // a Rentiq status change that never actually took effect.
            compensateKeycloakEnabledState(user.getId(), !targetEnabled, e);
            throw e;
        }

        return new AdminUserStatusResponse(
                user.getId(),
                previousStatus,
                user.getAccountStatus(),
                reason,
                user.getUpdatedAt()
        );
    }

    private User requireModeratableUser(String userId, String adminId) {
        User user = requireUser(userId);

        if (userId.equals(adminId)) {
            throw new ForbiddenException("User", "An admin cannot moderate their own account");
        }

        IdentitySnapshot identity = getIdentitySnapshot(userId);
        if (identity.roles().contains(RoleEnum.ADMIN.name())) {
            throw new ForbiddenException("User", "An admin cannot suspend, ban, or reinstate another admin");
        }

        return user;
    }

    private User requireUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        IdentitySnapshot identity = getIdentitySnapshot(user.getId());
        UserRepresentation kcUser = identity.user();

        return AdminUserResponse.builder()
                .id(user.getId())
                .username(kcUser.getUsername())
                .email(kcUser.getEmail())
                .firstName(kcUser.getFirstName())
                .lastName(kcUser.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .locale(user.getLocale())
                .accountStatus(user.getAccountStatus())
                .enabled(Boolean.TRUE.equals(kcUser.isEnabled()))
                .emailVerified(Boolean.TRUE.equals(kcUser.isEmailVerified()))
                .roles(identity.roles())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private VendorSeed toVendorSeedIfVendor(User user) {
        IdentitySnapshot identity = getIdentitySnapshot(user.getId());
        if (!identity.roles().contains(RoleEnum.VENDOR.name())) {
            return null;
        }
        return new VendorSeed(user, identity);
    }

    private AdminVendorResponse toAdminVendorResponse(VendorSeed seed) {
        User user = seed.user();
        UserRepresentation kcUser = seed.identity().user();

        OwnerWallet wallet = ownerWalletRepository.findByOwnerId(user.getId()).orElse(null);
        String kycStatus = userKycRepository.findByUserId(user.getId())
                .map(kyc -> kyc.getVerificationStatus())
                .orElse(KYC_NOT_SUBMITTED);

        BigDecimal averageRating = reviewRepository.calculateAverageRatingForOwner(user.getId());
        averageRating = averageRating == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : averageRating.setScale(2, RoundingMode.HALF_UP);

        return AdminVendorResponse.builder()
                .id(user.getId())
                .username(kcUser.getUsername())
                .email(kcUser.getEmail())
                .firstName(kcUser.getFirstName())
                .lastName(kcUser.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .accountStatus(user.getAccountStatus())
                .enabled(Boolean.TRUE.equals(kcUser.isEnabled()))
                .emailVerified(Boolean.TRUE.equals(kcUser.isEmailVerified()))
                .kycStatus(kycStatus)
                .walletBalance(wallet != null ? wallet.getBalance() : null)
                .walletCurrency(wallet != null ? wallet.getCurrency() : null)
                .walletStatus(wallet != null ? wallet.getStatus() : null)
                .totalItems(itemRepository.countByOwnerIdAndDeletedFalse(user.getId()))
                .totalBookings(bookingRepository.countByOwnerId(user.getId()))
                .averageRating(averageRating)
                .totalReviews(reviewRepository.countVisibleReviewsForOwner(user.getId()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private IdentitySnapshot getIdentitySnapshot(String userId) {
        try {
            UserResource userResource = keycloak.realm(keycloakProps.getTargetRealm())
                    .users()
                    .get(userId);

            UserRepresentation user = userResource.toRepresentation();
            List<RoleRepresentation> roleRepresentations = userResource.roles()
                    .realmLevel()
                    .listAll();

            Set<String> roles = roleRepresentations.stream()
                    .map(RoleRepresentation::getName)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

            return new IdentitySnapshot(user, Set.copyOf(roles));
        } catch (jakarta.ws.rs.NotFoundException e) {
            throw new NotFoundException("Identity user", userId);
        } catch (RuntimeException e) {
            throw new KeycloakOperationException("Failed to load identity data for user " + userId, e);
        }
    }

    private Pageable boundedPageable(Pageable pageable) {
        int size = Math.max(1, Math.min(pageable.getPageSize(), MAX_USER_PAGE_SIZE));
        return PageRequest.of(pageable.getPageNumber(), size, pageable.getSort());
    }

    private void syncKeycloakEnabledState(String userId, boolean enabled) {
        try {
            UserResource userResource = keycloak.realm(keycloakProps.getTargetRealm())
                    .users()
                    .get(userId);
            UserRepresentation representation = userResource.toRepresentation();
            representation.setEnabled(enabled);
            userResource.update(representation);
        } catch (jakarta.ws.rs.NotFoundException e) {
            throw new NotFoundException("Identity user", userId);
        } catch (RuntimeException e) {
            throw new KeycloakOperationException("Failed to update Keycloak access for user " + userId, e);
        }
    }

    private void compensateKeycloakEnabledState(String userId, boolean revertToEnabled, RuntimeException cause) {
        try {
            syncKeycloakEnabledState(userId, revertToEnabled);
            log.warn("Reverted Keycloak enabled={} for user {} after a moderation change failed to commit locally",
                    revertToEnabled, userId, cause);
        } catch (RuntimeException compensationFailure) {
            log.error("Keycloak enabled-state for user {} may now be out of sync with Rentiq after a failed " +
                    "moderation attempt; manual reconciliation required", userId, compensationFailure);
        }
    }

    private void revokeKeycloakSessionsQuietly(String userId) {
        try {
            keycloak.realm(keycloakProps.getTargetRealm())
                    .users()
                    .get(userId)
                    .logout();
        } catch (RuntimeException e) {
            log.warn("Account {} was disabled, but Keycloak session revocation failed", userId, e);
        }
    }

    private record IdentitySnapshot(UserRepresentation user, Set<String> roles) {
    }

    private record VendorSeed(User user, IdentitySnapshot identity) {
    }
}
