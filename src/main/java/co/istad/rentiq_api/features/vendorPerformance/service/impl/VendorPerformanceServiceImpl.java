package co.istad.rentiq_api.features.vendorPerformance.service.impl;

import co.istad.rentiq_api.common.config.props.KeycloakAdminClientProps;
import co.istad.rentiq_api.common.exception.ForbiddenException;
import co.istad.rentiq_api.common.exception.InvalidStateException;
import co.istad.rentiq_api.common.exception.NotFoundException;
import co.istad.rentiq_api.features.auth.RoleEnum;
import co.istad.rentiq_api.features.auth.exception.KeycloakOperationException;
import co.istad.rentiq_api.features.bookings.enums.BookingStatus;
import co.istad.rentiq_api.features.bookings.repository.BookingRepository;
import co.istad.rentiq_api.features.bookings.repository.BookingStatusHistoryRepository;
import co.istad.rentiq_api.features.review.repository.ReviewRepository;
import co.istad.rentiq_api.features.userProfile.entity.User;
import co.istad.rentiq_api.features.userProfile.enums.AccountStatus;
import co.istad.rentiq_api.features.userProfile.repository.UserRepository;
import co.istad.rentiq_api.features.vendorPerformance.dto.response.VendorModerationResponse;
import co.istad.rentiq_api.features.vendorPerformance.dto.response.VendorPerformanceResponse;
import co.istad.rentiq_api.features.vendorPerformance.entity.VendorStatusAudit;
import co.istad.rentiq_api.features.vendorPerformance.enums.VendorModerationAction;
import co.istad.rentiq_api.features.vendorPerformance.repository.VendorStatusAuditRepository;
import co.istad.rentiq_api.features.vendorPerformance.service.VendorPerformanceService;
import co.istad.rentiq_api.features.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorPerformanceServiceImpl implements VendorPerformanceService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final BookingStatusHistoryRepository historyRepository;
    private final ReviewRepository reviewRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final VendorStatusAuditRepository auditRepository;
    private final Keycloak keycloak;
    private final KeycloakAdminClientProps keycloakProps;

    @Override
    public VendorPerformanceResponse getPerformance(String ownerId) {

        User vendor = userRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Vendor", ownerId));

        long totalBookings = bookingRepository.countByOwnerId(ownerId);
        long completedBookings = bookingRepository.countByOwnerIdAndStatus(ownerId, BookingStatus.COMPLETED);

        long acceptedCount = historyRepository.countByBooking_OwnerIdAndOldStatusAndNewStatus(
                ownerId, BookingStatus.PENDING, BookingStatus.APPROVED);
        long rejectedCount = historyRepository.countByBooking_OwnerIdAndOldStatusAndNewStatus(
                ownerId, BookingStatus.PENDING, BookingStatus.REJECTED);
        long decidedCount = acceptedCount + rejectedCount;

        BigDecimal acceptanceRate = decidedCount == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(acceptedCount)
                        .divide(BigDecimal.valueOf(decidedCount), 4, RoundingMode.HALF_UP);

        long cancelledCount = historyRepository.countByBooking_OwnerIdAndNewStatus(ownerId, BookingStatus.CANCELLED);

        BigDecimal cancellationRate = totalBookings == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(cancelledCount)
                        .divide(BigDecimal.valueOf(totalBookings), 4, RoundingMode.HALF_UP);

        BigDecimal averageRating = reviewRepository.calculateAverageRatingForOwner(ownerId);
        averageRating = averageRating == null ? BigDecimal.ZERO : averageRating.setScale(2, RoundingMode.HALF_UP);
        long reviewCount = reviewRepository.countVisibleReviewsForOwner(ownerId);

        Double medianResponseSeconds = historyRepository.findMedianResponseSeconds(ownerId);
        BigDecimal medianResponseTimeMinutes = medianResponseSeconds == null
                ? null
                : BigDecimal.valueOf(medianResponseSeconds / 60.0).setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalEarnings = walletTransactionRepository.sumBookingEarningsForOwner(ownerId);
        totalEarnings = totalEarnings == null ? BigDecimal.ZERO : totalEarnings;

        return new VendorPerformanceResponse(
                ownerId,
                vendor.getAccountStatus(),
                totalBookings,
                completedBookings,
                acceptanceRate,
                cancellationRate,
                averageRating,
                reviewCount,
                medianResponseTimeMinutes,
                totalEarnings);
    }

    @Override
    @Transactional
    public VendorModerationResponse suspend(String targetId, String reason, String adminId) {
        User target = requireTargetForModeration(targetId, adminId);

        if (target.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new InvalidStateException(
                    "Vendor", target.getAccountStatus(), "Vendor is already " + target.getAccountStatus());
        }

        return applyModeration(target, AccountStatus.SUSPENDED, VendorModerationAction.SUSPEND, reason, adminId, true);
    }

    @Override
    @Transactional
    public VendorModerationResponse ban(String targetId, String reason, String adminId) {
        User target = requireTargetForModeration(targetId, adminId);

        if (target.getAccountStatus() == AccountStatus.BANNED) {
            throw new InvalidStateException("Vendor", target.getAccountStatus(), "Vendor is already banned");
        }

        return applyModeration(target, AccountStatus.BANNED, VendorModerationAction.BAN, reason, adminId, true);
    }

    @Override
    @Transactional
    public VendorModerationResponse reinstate(String targetId, String reason, String adminId) {
        User target = requireTargetForModeration(targetId, adminId);

        if (target.getAccountStatus() == AccountStatus.ACTIVE) {
            throw new InvalidStateException("Vendor", target.getAccountStatus(), "Vendor is already active");
        }

        return applyModeration(target, AccountStatus.ACTIVE, VendorModerationAction.REINSTATE, reason, adminId, false);
    }

    private VendorModerationResponse applyModeration(
            User target,
            AccountStatus newStatus,
            VendorModerationAction action,
            String reason,
            String adminId,
            boolean revokeSession
    ) {
        target.setAccountStatus(newStatus);
        userRepository.save(target);

        VendorStatusAudit audit = auditRepository.save(VendorStatusAudit.builder()
                .adminId(adminId)
                .targetId(target.getId())
                .action(action)
                .reason(reason)
                .build());

        if (revokeSession) {
            revokeKeycloakSession(target.getId());
        }

        return new VendorModerationResponse(
                target.getId(), target.getAccountStatus(), action, reason, adminId, audit.getCreatedAt());
    }

    private User requireTargetForModeration(String targetId, String adminId) {
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new NotFoundException("Vendor", targetId));

        if (targetId.equals(adminId)) {
            throw new ForbiddenException("Vendor", "An admin cannot moderate their own account");
        }

        if (hasAdminRole(targetId)) {
            throw new ForbiddenException("Vendor", "An admin cannot suspend, ban, or reinstate another admin");
        }

        return target;
    }

    private boolean hasAdminRole(String userId) {
        try {
            List<RoleRepresentation> roles = keycloak.realm(keycloakProps.getTargetRealm())
                    .users()
                    .get(userId)
                    .roles()
                    .realmLevel()
                    .listAll();

            return roles.stream().anyMatch(role -> RoleEnum.ADMIN.name().equals(role.getName()));
        } catch (RuntimeException e) {
            throw new KeycloakOperationException("Failed to look up roles for user " + userId, e);
        }
    }

    private void revokeKeycloakSession(String userId) {
        try {
            UserResource userResource = keycloak.realm(keycloakProps.getTargetRealm()).users().get(userId);
            userResource.logout();
        } catch (RuntimeException e) {
            throw new KeycloakOperationException("Failed to revoke sessions for user " + userId, e);
        }
    }
}
