package co.istad.rentiq_api.features.vendorPerformance.service.impl;

import co.istad.rentiq_api.common.exception.ForbiddenException;
import co.istad.rentiq_api.common.exception.InvalidStateException;
import co.istad.rentiq_api.features.adminUserManagement.dto.response.AdminUserStatusResponse;
import co.istad.rentiq_api.features.adminUserManagement.service.AdminUserManagementService;
import co.istad.rentiq_api.features.bookings.enums.BookingStatus;
import co.istad.rentiq_api.features.bookings.repository.BookingRepository;
import co.istad.rentiq_api.features.bookings.repository.BookingStatusHistoryRepository;
import co.istad.rentiq_api.features.review.repository.ReviewRepository;
import co.istad.rentiq_api.features.userProfile.entity.User;
import co.istad.rentiq_api.features.userProfile.enums.AccountStatus;
import co.istad.rentiq_api.features.userProfile.repository.UserRepository;
import co.istad.rentiq_api.features.vendorPerformance.dto.response.VendorModerationResponse;
import co.istad.rentiq_api.features.vendorPerformance.dto.response.VendorPerformanceResponse;
import co.istad.rentiq_api.features.vendorPerformance.enums.VendorModerationAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Backend audit P0-2 — VendorPerformanceServiceImpl no longer owns any moderation business
 * logic; suspend/ban/reinstate must delegate to AdminUserManagementService (the single
 * canonical implementation) and only translate the response shape. Backend audit FIN-004 also
 * covers getPerformance's Booking-sourced (never Wallet-sourced) completedBookingValue.
 */
@ExtendWith(MockitoExtension.class)
class VendorPerformanceServiceImplTest {

    private static final String OWNER_ID = "vendor-1";
    private static final String ADMIN_ID = "admin-1";

    @Mock private UserRepository userRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private BookingStatusHistoryRepository historyRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private AdminUserManagementService adminUserManagementService;

    private VendorPerformanceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VendorPerformanceServiceImpl(
                userRepository, bookingRepository, historyRepository, reviewRepository,
                adminUserManagementService);

        User vendor = User.builder().id(OWNER_ID).accountStatus(AccountStatus.ACTIVE).build();
        lenient().when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(vendor));
        lenient().when(bookingRepository.countByOwnerId(OWNER_ID)).thenReturn(0L);
        lenient().when(bookingRepository.countByOwnerIdAndStatus(any(), any())).thenReturn(0L);
        lenient().when(historyRepository.countByBooking_OwnerIdAndOldStatusAndNewStatus(any(), any(), any())).thenReturn(0L);
        lenient().when(historyRepository.countByBooking_OwnerIdAndNewStatus(any(), any())).thenReturn(0L);
        lenient().when(reviewRepository.calculateAverageRatingForOwner(OWNER_ID)).thenReturn(null);
        lenient().when(reviewRepository.countVisibleReviewsForOwner(OWNER_ID)).thenReturn(0L);
        lenient().when(historyRepository.findMedianResponseSeconds(OWNER_ID)).thenReturn(null);
    }

    @Test
    void getPerformance_sourcesCompletedBookingValue_fromBookingSubtotal_neverFromWallet() {
        when(bookingRepository.sumSubtotalByOwnerIdAndStatus(OWNER_ID, BookingStatus.COMPLETED))
                .thenReturn(new BigDecimal("2500.00"));

        VendorPerformanceResponse response = service.getPerformance(OWNER_ID);

        assertThat(response.completedBookingValue()).isEqualByComparingTo("2500.00");
        verify(bookingRepository).sumSubtotalByOwnerIdAndStatus(OWNER_ID, BookingStatus.COMPLETED);
        // No wallet repository is even injected into this service any more — the only possible
        // source for completedBookingValue is Booking, proven structurally by the constructor
        // signature used above (no WalletTransactionRepository parameter exists to mock).
        verifyNoInteractions(adminUserManagementService);
    }

    @Test
    void getPerformance_returnsZero_whenVendorHasNoCompletedBookings() {
        when(bookingRepository.sumSubtotalByOwnerIdAndStatus(OWNER_ID, BookingStatus.COMPLETED))
                .thenReturn(null);

        VendorPerformanceResponse response = service.getPerformance(OWNER_ID);

        assertThat(response.completedBookingValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void suspend_delegatesToAdminUserManagementService_suspendVendor() {
        when(adminUserManagementService.suspendVendor(OWNER_ID, "policy violation", ADMIN_ID))
                .thenReturn(new AdminUserStatusResponse(
                        OWNER_ID, AccountStatus.ACTIVE, AccountStatus.SUSPENDED, "policy violation",
                        OffsetDateTime.now()));

        VendorModerationResponse response = service.suspend(OWNER_ID, "policy violation", ADMIN_ID);

        verify(adminUserManagementService).suspendVendor(OWNER_ID, "policy violation", ADMIN_ID);
        assertThat(response.targetId()).isEqualTo(OWNER_ID);
        assertThat(response.accountStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(response.action()).isEqualTo(VendorModerationAction.SUSPEND);
        assertThat(response.adminId()).isEqualTo(ADMIN_ID);
    }

    @Test
    void ban_delegatesToAdminUserManagementService_banVendor() {
        when(adminUserManagementService.banVendor(OWNER_ID, "fraud", ADMIN_ID))
                .thenReturn(new AdminUserStatusResponse(
                        OWNER_ID, AccountStatus.ACTIVE, AccountStatus.BANNED, "fraud", OffsetDateTime.now()));

        VendorModerationResponse response = service.ban(OWNER_ID, "fraud", ADMIN_ID);

        verify(adminUserManagementService).banVendor(OWNER_ID, "fraud", ADMIN_ID);
        assertThat(response.accountStatus()).isEqualTo(AccountStatus.BANNED);
        assertThat(response.action()).isEqualTo(VendorModerationAction.BAN);
    }

    @Test
    void reinstate_delegatesToAdminUserManagementService_reinstateVendor() {
        when(adminUserManagementService.reinstateVendor(OWNER_ID, "appeal approved", ADMIN_ID))
                .thenReturn(new AdminUserStatusResponse(
                        OWNER_ID, AccountStatus.SUSPENDED, AccountStatus.ACTIVE, "appeal approved",
                        OffsetDateTime.now()));

        VendorModerationResponse response = service.reinstate(OWNER_ID, "appeal approved", ADMIN_ID);

        verify(adminUserManagementService).reinstateVendor(OWNER_ID, "appeal approved", ADMIN_ID);
        assertThat(response.accountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(response.action()).isEqualTo(VendorModerationAction.REINSTATE);
    }

    @Test
    void suspend_propagatesInvalidStateException_forRepeatedOperation() {
        when(adminUserManagementService.suspendVendor(OWNER_ID, "reason", ADMIN_ID))
                .thenThrow(new InvalidStateException("Vendor", AccountStatus.SUSPENDED, "Vendor is already suspended"));

        assertThatThrownBy(() -> service.suspend(OWNER_ID, "reason", ADMIN_ID))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    void ban_propagatesForbiddenException_whenAdminAttemptsSelfModeration() {
        when(adminUserManagementService.banVendor(ADMIN_ID, "reason", ADMIN_ID))
                .thenThrow(new ForbiddenException("Vendor", "An admin cannot moderate their own account"));

        assertThatThrownBy(() -> service.ban(ADMIN_ID, "reason", ADMIN_ID))
                .isInstanceOf(ForbiddenException.class);
    }
}
