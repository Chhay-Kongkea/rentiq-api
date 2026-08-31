package co.istad.rentiq_api.features.vendorPerformance.service.impl;

import co.istad.rentiq_api.common.config.props.KeycloakAdminClientProps;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
import co.istad.rentiq_api.features.bookings.enums.BookingStatus;
import co.istad.rentiq_api.features.bookings.repository.BookingRepository;
import co.istad.rentiq_api.features.bookings.repository.BookingStatusHistoryRepository;
import co.istad.rentiq_api.features.review.repository.ReviewRepository;
import co.istad.rentiq_api.features.userProfile.entity.User;
import co.istad.rentiq_api.features.userProfile.enums.AccountStatus;
import co.istad.rentiq_api.features.userProfile.repository.UserRepository;
import co.istad.rentiq_api.features.vendorPerformance.dto.response.VendorPerformanceResponse;
import co.istad.rentiq_api.features.vendorPerformance.repository.VendorStatusAuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Backend audit FIN-004 — Vendor Performance's earnings figure must no longer depend on the
 * obsolete WalletTransaction.bookingId/BOOKING_EARNING path (rental payment is P2P and never
 * touches the wallet ledger); it must instead be Booking-sourced GMV
 * ({@code completedBookingValue}).
 */
@ExtendWith(MockitoExtension.class)
class VendorPerformanceServiceImplTest {

    private static final String OWNER_ID = "vendor-1";

    @Mock private UserRepository userRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private BookingStatusHistoryRepository historyRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private VendorStatusAuditRepository auditRepository;
    @Mock private Keycloak keycloak;
    @Mock private KeycloakAdminClientProps keycloakProps;
    @Mock private AdminAuditService adminAuditService;

    private VendorPerformanceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VendorPerformanceServiceImpl(
                userRepository, bookingRepository, historyRepository, reviewRepository,
                auditRepository, keycloak, keycloakProps, adminAuditService);

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
        verifyNoInteractions(adminAuditService);
    }

    @Test
    void getPerformance_returnsZero_whenVendorHasNoCompletedBookings() {
        when(bookingRepository.sumSubtotalByOwnerIdAndStatus(OWNER_ID, BookingStatus.COMPLETED))
                .thenReturn(null);

        VendorPerformanceResponse response = service.getPerformance(OWNER_ID);

        assertThat(response.completedBookingValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
