package co.istad.rentiq_api.features.adminDashboard.service.impl;

import co.istad.rentiq_api.common.exception.InvalidOperationException;
import co.istad.rentiq_api.features.adminDashboard.dto.response.AdminDashboardResponse;
import co.istad.rentiq_api.features.adminDashboard.dto.response.DashboardCountTrendResponse;
import co.istad.rentiq_api.features.adminDashboard.dto.response.DashboardTrendResponse;
import co.istad.rentiq_api.features.adminDashboard.dto.response.RecentDashboardActivityResponse;
import co.istad.rentiq_api.features.adminDashboard.dto.response.UserDashboardSummaryResponse;
import co.istad.rentiq_api.features.adminDashboard.projection.DashboardCountProjection;
import co.istad.rentiq_api.features.adminDashboard.projection.DashboardFinancialProjection;
import co.istad.rentiq_api.features.bookingDispute.repository.BookingDisputeRepository;
import co.istad.rentiq_api.features.bookings.enums.BookingStatus;
import co.istad.rentiq_api.features.bookings.repository.BookingRepository;
import co.istad.rentiq_api.features.financialReport.dto.GroupBy;
import co.istad.rentiq_api.features.financialReport.dto.projection.BookingPeriodAggregateProjection;
import co.istad.rentiq_api.features.financialReport.dto.projection.BookingTotalsProjection;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueCurrencySummary;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueSummaryResponse;
import co.istad.rentiq_api.features.financialReport.service.FinancialReportService;
import co.istad.rentiq_api.features.item.enums.ItemApprovalStatus;
import co.istad.rentiq_api.features.item.repository.ItemRepository;
import co.istad.rentiq_api.features.kyc.repository.UserKycRepository;
import co.istad.rentiq_api.features.report.enums.ReportStatus;
import co.istad.rentiq_api.features.report.repository.ReportRepository;
import co.istad.rentiq_api.features.userProfile.entity.User;
import co.istad.rentiq_api.features.userProfile.enums.AccountStatus;
import co.istad.rentiq_api.features.userProfile.repository.UserRepository;
import co.istad.rentiq_api.features.vendorApplication.entity.VendorApplication;
import co.istad.rentiq_api.features.vendorApplication.enums.VendorApplicationStatus;
import co.istad.rentiq_api.features.vendorApplication.repository.VendorApplicationRepository;
import co.istad.rentiq_api.features.wallet.enums.TopupStatus;
import co.istad.rentiq_api.features.wallet.repository.TopupRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private VendorApplicationRepository vendorApplicationRepository;
    @Mock private UserKycRepository userKycRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private BookingDisputeRepository disputeRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private TopupRequestRepository topupRequestRepository;
    @Mock private FinancialReportService financialReportService;

    private AdminDashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminDashboardServiceImpl(
                userRepository, vendorApplicationRepository, userKycRepository,
                itemRepository, bookingRepository, disputeRepository,
                reportRepository, topupRequestRepository, financialReportService);

        // Common no-op stubs so every test doesn't need to restate the full dashboard graph.
        lenient().when(userRepository.count()).thenReturn(0L);
        lenient().when(userRepository.countByAccountStatus(any())).thenReturn(0L);
        lenient().when(userRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any())).thenReturn(0L);
        lenient().when(vendorApplicationRepository.countApprovedVendorsByAccountStatus(any(), any())).thenReturn(0L);
        lenient().when(vendorApplicationRepository.countByStatus(any())).thenReturn(0L);
        lenient().when(itemRepository.countByDeletedFalse()).thenReturn(0L);
        lenient().when(itemRepository.countByDeletedFalseAndStatus(any())).thenReturn(0L);
        lenient().when(itemRepository.countByDeletedFalseAndApprovalStatus(any())).thenReturn(0L);
        lenient().when(itemRepository.countCurrentlyFeatured(any())).thenReturn(0L);
        lenient().when(bookingRepository.count()).thenReturn(0L);
        lenient().when(bookingRepository.countByStatusIn(anyList())).thenReturn(0L);
        lenient().when(bookingRepository.countByStatus(any())).thenReturn(0L);
        lenient().when(disputeRepository.countByStatus(anyString())).thenReturn(0L);
        lenient().when(disputeRepository.countDistinctBookingIdByStatus(anyString())).thenReturn(0L);
        lenient().when(userKycRepository.countByVerificationStatus(anyString())).thenReturn(0L);
        lenient().when(reportRepository.countByStatusIn(any())).thenReturn(0L);
        lenient().when(topupRequestRepository.countByStatus(any())).thenReturn(0L);
        // A JPQL aggregate query without GROUP BY always yields exactly one row
        // (that's why the underlying queries use coalesce(sum(...), 0)), so the
        // "no data" case is a zero-valued projection, never a null one.
        DashboardFinancialProjection zeroFinancials = mock(DashboardFinancialProjection.class);
        lenient().when(zeroFinancials.getTotalBookingValue()).thenReturn(BigDecimal.ZERO);
        lenient().when(zeroFinancials.getCalculatedCommission()).thenReturn(BigDecimal.ZERO);
        lenient().when(bookingRepository.sumDashboardFinancials()).thenReturn(zeroFinancials);

        BookingTotalsProjection zeroTotals = mock(BookingTotalsProjection.class);
        lenient().when(zeroTotals.getTotalBookingValue()).thenReturn(BigDecimal.ZERO);
        lenient().when(bookingRepository.sumBookingTotals(any(), any())).thenReturn(zeroTotals);

        // Ledger-backed Platform Revenue — reused from FinancialReportService, never a second
        // formula. Default to an empty-but-present currency list (no PROMOTION/ADVERTISEMENT
        // charges yet), matching how the real service behaves with no wallet activity.
        lenient().when(financialReportService.getPlatformRevenueSummary(any(), any()))
                .thenReturn(new PlatformRevenueSummaryResponse(null, null, List.of()));
    }

    // ---------------------------------------------------------------
    // Main dashboard
    // ---------------------------------------------------------------

    @Test
    void getDashboard_aggregatesCountsAndFinancialsFromRepositories() {
        when(userRepository.count()).thenReturn(1250L);
        when(userRepository.countByAccountStatus(AccountStatus.ACTIVE)).thenReturn(1190L);
        when(userRepository.countByAccountStatus(AccountStatus.SUSPENDED)).thenReturn(40L);
        when(userRepository.countByAccountStatus(AccountStatus.BANNED)).thenReturn(20L);
        when(userRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any())).thenReturn(14L);

        when(vendorApplicationRepository.countApprovedVendorsByAccountStatus(VendorApplicationStatus.APPROVED, AccountStatus.ACTIVE))
                .thenReturn(163L);
        when(vendorApplicationRepository.countApprovedVendorsByAccountStatus(VendorApplicationStatus.APPROVED, AccountStatus.SUSPENDED))
                .thenReturn(14L);
        when(vendorApplicationRepository.countApprovedVendorsByAccountStatus(VendorApplicationStatus.APPROVED, AccountStatus.BANNED))
                .thenReturn(8L);
        when(vendorApplicationRepository.countByStatus(VendorApplicationStatus.PENDING)).thenReturn(12L);

        when(itemRepository.countByDeletedFalse()).thenReturn(2450L);
        when(itemRepository.countByDeletedFalseAndStatus(any())).thenReturn(2205L);
        when(itemRepository.countByDeletedFalseAndApprovalStatus(ItemApprovalStatus.PENDING)).thenReturn(67L);
        when(itemRepository.countByDeletedFalseAndApprovalStatus(ItemApprovalStatus.REJECTED)).thenReturn(98L);
        when(itemRepository.countCurrentlyFeatured(any())).thenReturn(80L);

        when(bookingRepository.count()).thenReturn(8340L);
        when(bookingRepository.countByStatusIn(anyList())).thenReturn(174L);
        when(bookingRepository.countByStatus(BookingStatus.COMPLETED)).thenReturn(7640L);
        when(bookingRepository.countByStatus(BookingStatus.CANCELLED)).thenReturn(498L);
        when(disputeRepository.countDistinctBookingIdByStatus("OPEN")).thenReturn(28L);
        when(disputeRepository.countByStatus("OPEN")).thenReturn(5L);

        when(userKycRepository.countByVerificationStatus("PENDING")).thenReturn(18L);
        when(reportRepository.countByStatusIn(List.of(ReportStatus.OPEN, ReportStatus.UNDER_REVIEW))).thenReturn(14L);
        when(topupRequestRepository.countByStatus(TopupStatus.PENDING)).thenReturn(9L);

        DashboardFinancialProjection allTime = mock(DashboardFinancialProjection.class);
        when(allTime.getTotalBookingValue()).thenReturn(new BigDecimal("184500.00"));
        when(allTime.getCalculatedCommission()).thenReturn(new BigDecimal("17240.00"));
        when(bookingRepository.sumDashboardFinancials()).thenReturn(allTime);

        BookingTotalsProjection today = mock(BookingTotalsProjection.class);
        when(today.getTotalBookingValue()).thenReturn(new BigDecimal("485.50"));
        when(bookingRepository.sumBookingTotals(any(), any())).thenReturn(today);

        PlatformRevenueCurrencySummary usdRevenue = new PlatformRevenueCurrencySummary(
                "USD", new BigDecimal("18450.00"), new BigDecimal("12000.00"), new BigDecimal("6450.00"), 40L, 25L, 15L);
        when(financialReportService.getPlatformRevenueSummary(any(), any()))
                .thenReturn(new PlatformRevenueSummaryResponse(null, null, List.of(usdRevenue)));

        AdminDashboardResponse response = service.getDashboard();

        assertThat(response.users()).isEqualTo(new UserDashboardSummaryResponse(1250L, 1190L, 40L, 20L, 14L));
        assertThat(response.vendors().total()).isEqualTo(163L + 14L + 8L);
        assertThat(response.vendors().pendingApplications()).isEqualTo(12L);
        assertThat(response.listings().pendingApproval()).isEqualTo(67L);
        assertThat(response.listings().featured()).isEqualTo(80L);
        assertThat(response.bookings().disputed()).isEqualTo(28L);
        assertThat(response.pendingActions().disputes()).isEqualTo(5L);
        assertThat(response.pendingActions().kycSubmissions()).isEqualTo(18L);
        assertThat(response.pendingActions().reports()).isEqualTo(14L);
        assertThat(response.pendingActions().topUps()).isEqualTo(9L);
        assertThat(response.financial().totalBookingValue()).isEqualByComparingTo("184500.00");
        assertThat(response.financial().calculatedCommission()).isEqualByComparingTo("17240.00");
        assertThat(response.financial().todayBookingValue()).isEqualByComparingTo("485.50");
        assertThat(response.financial().platformRevenue()).hasSize(1);
        assertThat(response.financial().platformRevenue().get(0).currency()).isEqualTo("USD");
        assertThat(response.financial().platformRevenue().get(0).totalRevenue()).isEqualByComparingTo("18450.00");
    }

    @Test
    void getDashboard_returnsZeroInsteadOfNull_whenNoDataExists() {
        AdminDashboardResponse response = service.getDashboard();

        assertThat(response.users().total()).isZero();
        assertThat(response.vendors().total()).isZero();
        assertThat(response.listings().total()).isZero();
        assertThat(response.bookings().total()).isZero();
        assertThat(response.pendingActions().disputes()).isZero();
        assertThat(response.financial().totalBookingValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.financial().calculatedCommission()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.financial().todayBookingValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.financial().platformRevenue()).isEmpty();
    }

    // ---------------------------------------------------------------
    // Trend validation
    // ---------------------------------------------------------------

    @Test
    void getRevenueTrend_rejectsFromAfterTo() {
        assertThatThrownBy(() -> service.getRevenueTrend(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 1, 1), GroupBy.DAY))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void getRevenueTrend_rejectsRangeExceedingOneYear() {
        assertThatThrownBy(() -> service.getRevenueTrend(
                LocalDate.of(2024, 1, 1), LocalDate.of(2026, 1, 2), GroupBy.DAY))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void getRevenueTrend_requiresFromDate() {
        assertThatThrownBy(() -> service.getRevenueTrend(null, LocalDate.now(), GroupBy.DAY))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void getRevenueTrend_dayGrouping_zeroFillsMissingDays() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 3);

        BookingPeriodAggregateProjection middleDay = mock(BookingPeriodAggregateProjection.class);
        when(middleDay.getPeriod()).thenReturn(Date.valueOf(LocalDate.of(2026, 1, 2)));
        when(middleDay.getTotalBookingValue()).thenReturn(new BigDecimal("100.00"));

        when(bookingRepository.aggregateBookingTotalsByDay(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(middleDay)));

        DashboardTrendResponse trend = service.getRevenueTrend(from, to, GroupBy.DAY);

        assertThat(trend.data()).hasSize(3);
        assertThat(trend.data().get(0).period()).isEqualTo("2026-01-01");
        assertThat(trend.data().get(0).value()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(trend.data().get(1).period()).isEqualTo("2026-01-02");
        assertThat(trend.data().get(1).value()).isEqualByComparingTo("100.00");
        assertThat(trend.data().get(2).period()).isEqualTo("2026-01-03");
        assertThat(trend.data().get(2).value()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getBookingTrend_monthGrouping_zeroFillsMissingMonths() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 1);

        DashboardCountProjection jan = mock(DashboardCountProjection.class);
        when(jan.getPeriod()).thenReturn(Date.valueOf(LocalDate.of(2026, 1, 1)));
        when(jan.getValue()).thenReturn(460L);

        DashboardCountProjection mar = mock(DashboardCountProjection.class);
        when(mar.getPeriod()).thenReturn(Date.valueOf(LocalDate.of(2026, 3, 1)));
        when(mar.getValue()).thenReturn(590L);

        when(bookingRepository.countBookingsByPeriod(any(), any(), anyString()))
                .thenReturn(List.of(jan, mar));

        DashboardCountTrendResponse trend = service.getBookingTrend(from, to, GroupBy.MONTH);

        assertThat(trend.data()).hasSize(3);
        assertThat(trend.data().get(0).period()).isEqualTo("2026-01");
        assertThat(trend.data().get(0).value()).isEqualTo(460L);
        assertThat(trend.data().get(1).period()).isEqualTo("2026-02");
        assertThat(trend.data().get(1).value()).isEqualTo(0L);
        assertThat(trend.data().get(2).period()).isEqualTo("2026-03");
        assertThat(trend.data().get(2).value()).isEqualTo(590L);
    }

    @Test
    void getUserGrowth_defaultsToToday_whenToIsOmitted() {
        when(userRepository.countRegistrationsByPeriod(any(), any(), anyString())).thenReturn(List.of());

        DashboardCountTrendResponse trend = service.getUserGrowth(LocalDate.now(ZoneOffset.UTC), null, GroupBy.DAY);

        assertThat(trend.data()).hasSize(1);
        assertThat(trend.data().get(0).value()).isZero();
    }

    // ---------------------------------------------------------------
    // Recent activity
    // ---------------------------------------------------------------

    @Test
    void getRecentActivity_sortsNewestFirstAcrossSources_andRespectsLimit() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        User oldestUser = User.builder().id("u1").accountStatus(AccountStatus.ACTIVE).createdAt(now.minusDays(3)).build();
        User newestUser = User.builder().id("u2").accountStatus(AccountStatus.ACTIVE).createdAt(now).build();
        when(userRepository.findAllByOrderByCreatedAtDesc(any()))
                .thenReturn(new PageImpl<>(List.of(newestUser, oldestUser)));

        VendorApplication application = VendorApplication.builder()
                .id(UUID.randomUUID()).status(VendorApplicationStatus.PENDING).createdAt(now.minusDays(1)).build();
        when(vendorApplicationRepository.findAllByOrderByCreatedAtDesc(any()))
                .thenReturn(new PageImpl<>(List.of(application)));

        when(userKycRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(Page.empty());
        when(itemRepository.findAllByDeletedFalseOrderByCreatedAtDesc(any())).thenReturn(Page.empty());
        when(bookingRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(Page.empty());
        when(disputeRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(Page.empty());
        when(reportRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(Page.empty());
        when(topupRequestRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(Page.empty());

        List<RecentDashboardActivityResponse> activity = service.getRecentActivity(2);

        assertThat(activity).hasSize(2);
        assertThat(activity.get(0).referenceId()).isEqualTo("u2");
        assertThat(activity.get(1).referenceId()).isEqualTo(application.getId().toString());
    }

    @Test
    void getRecentActivity_returnsEmptyList_whenAllSourcesEmpty() {
        when(userRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(Page.empty());
        when(vendorApplicationRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(Page.empty());
        when(userKycRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(Page.empty());
        when(itemRepository.findAllByDeletedFalseOrderByCreatedAtDesc(any())).thenReturn(Page.empty());
        when(bookingRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(Page.empty());
        when(disputeRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(Page.empty());
        when(reportRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(Page.empty());
        when(topupRequestRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(Page.empty());

        assertThat(service.getRecentActivity(10)).isEmpty();
    }

    @Test
    void getRecentActivity_rejectsLimitOutsideAllowedRange() {
        assertThatThrownBy(() -> service.getRecentActivity(0)).isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> service.getRecentActivity(101)).isInstanceOf(InvalidOperationException.class);
    }
}
