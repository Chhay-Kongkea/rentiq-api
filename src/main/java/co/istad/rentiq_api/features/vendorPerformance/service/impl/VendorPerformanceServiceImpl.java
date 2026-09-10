package co.istad.rentiq_api.features.vendorPerformance.service.impl;

import co.istad.rentiq_api.common.exception.NotFoundException;
import co.istad.rentiq_api.features.adminUserManagement.dto.response.AdminUserStatusResponse;
import co.istad.rentiq_api.features.adminUserManagement.service.AdminUserManagementService;
import co.istad.rentiq_api.features.bookings.enums.BookingStatus;
import co.istad.rentiq_api.features.bookings.repository.BookingRepository;
import co.istad.rentiq_api.features.bookings.repository.BookingStatusHistoryRepository;
import co.istad.rentiq_api.features.review.repository.ReviewRepository;
import co.istad.rentiq_api.features.userProfile.entity.User;
import co.istad.rentiq_api.features.userProfile.repository.UserRepository;
import co.istad.rentiq_api.features.vendorPerformance.dto.response.VendorModerationResponse;
import co.istad.rentiq_api.features.vendorPerformance.dto.response.VendorPerformanceResponse;
import co.istad.rentiq_api.features.vendorPerformance.enums.VendorModerationAction;
import co.istad.rentiq_api.features.vendorPerformance.service.VendorPerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Backend audit P0-2 — vendor moderation (suspend/ban/reinstate) used to be duplicated here
 * with its own logic that never disabled the vendor's Keycloak account. That business logic
 * now lives solely in {@link AdminUserManagementService} (suspendVendor/banVendor/reinstateVendor);
 * this class only translates its response shape for the /admin/vendors route, which is kept so
 * existing API consumers of that route are not broken.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorPerformanceServiceImpl implements VendorPerformanceService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final BookingStatusHistoryRepository historyRepository;
    private final ReviewRepository reviewRepository;
    private final AdminUserManagementService adminUserManagementService;

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

        // Rental GMV arranged through Rentiq for this vendor's completed bookings — computed
        // directly from Booking, never from the Wallet ledger (rental payment is P2P and never
        // touches Rentiq; see FIN-004 in the backend audit).
        BigDecimal completedBookingValue = bookingRepository.sumSubtotalByOwnerIdAndStatus(ownerId, BookingStatus.COMPLETED);
        completedBookingValue = completedBookingValue == null ? BigDecimal.ZERO : completedBookingValue;

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
                completedBookingValue);
    }

    @Override
    @Transactional
    public VendorModerationResponse suspend(String targetId, String reason, String adminId) {
        return toVendorModerationResponse(
                adminUserManagementService.suspendVendor(targetId, reason, adminId),
                VendorModerationAction.SUSPEND, adminId);
    }

    @Override
    @Transactional
    public VendorModerationResponse ban(String targetId, String reason, String adminId) {
        return toVendorModerationResponse(
                adminUserManagementService.banVendor(targetId, reason, adminId),
                VendorModerationAction.BAN, adminId);
    }

    @Override
    @Transactional
    public VendorModerationResponse reinstate(String targetId, String reason, String adminId) {
        return toVendorModerationResponse(
                adminUserManagementService.reinstateVendor(targetId, reason, adminId),
                VendorModerationAction.REINSTATE, adminId);
    }

    private VendorModerationResponse toVendorModerationResponse(
            AdminUserStatusResponse response,
            VendorModerationAction action,
            String adminId
    ) {
        return new VendorModerationResponse(
                response.userId(),
                response.accountStatus(),
                action,
                response.reason(),
                adminId,
                response.updatedAt());
    }
}
