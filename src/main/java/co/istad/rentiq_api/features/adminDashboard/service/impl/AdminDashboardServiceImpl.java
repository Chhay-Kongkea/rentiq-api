package co.istad.rentiq_api.features.adminDashboard.service.impl;

import co.istad.rentiq_api.common.exception.InvalidOperationException;
import co.istad.rentiq_api.features.adminDashboard.dto.response.*;
import co.istad.rentiq_api.features.adminDashboard.enums.DashboardActivityType;
import co.istad.rentiq_api.features.adminDashboard.projection.DashboardCountProjection;
import co.istad.rentiq_api.features.adminDashboard.projection.DashboardFinancialProjection;
import co.istad.rentiq_api.features.adminDashboard.service.AdminDashboardService;
import co.istad.rentiq_api.features.bookingDispute.repository.BookingDisputeRepository;
import co.istad.rentiq_api.features.bookings.enums.BookingStatus;
import co.istad.rentiq_api.features.bookings.repository.BookingRepository;
import co.istad.rentiq_api.features.financialReport.dto.GroupBy;
import co.istad.rentiq_api.features.financialReport.dto.projection.BookingPeriodAggregateProjection;
import co.istad.rentiq_api.features.financialReport.dto.projection.BookingTotalsProjection;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueCurrencySummary;
import co.istad.rentiq_api.features.financialReport.service.FinancialReportService;
import co.istad.rentiq_api.features.item.enums.ItemApprovalStatus;
import co.istad.rentiq_api.features.item.enums.ItemStatus;
import co.istad.rentiq_api.features.item.repository.ItemRepository;
import co.istad.rentiq_api.features.kyc.KycStatus;
import co.istad.rentiq_api.features.kyc.repository.UserKycRepository;
import co.istad.rentiq_api.features.report.enums.ReportStatus;
import co.istad.rentiq_api.features.report.repository.ReportRepository;
import co.istad.rentiq_api.features.userProfile.enums.AccountStatus;
import co.istad.rentiq_api.features.userProfile.repository.UserRepository;
import co.istad.rentiq_api.features.vendorApplication.enums.VendorApplicationStatus;
import co.istad.rentiq_api.features.vendorApplication.repository.VendorApplicationRepository;
import co.istad.rentiq_api.features.wallet.enums.TopupStatus;
import co.istad.rentiq_api.features.wallet.repository.TopupRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final int MAX_TREND_RANGE_YEARS = 1;
    private static final int MAX_RECENT_ACTIVITY_LIMIT = 100;
    private static final int MAX_TREND_PERIODS = 500;
    private static final ZoneOffset REPORTING_ZONE = ZoneOffset.UTC;
    private static final List<BookingStatus> ACTIVE_BOOKING_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.APPROVED, BookingStatus.RENTED);
    private static final List<ReportStatus> PENDING_REPORT_STATUSES =
            List.of(ReportStatus.OPEN, ReportStatus.UNDER_REVIEW);

    private final UserRepository userRepository;
    private final VendorApplicationRepository vendorApplicationRepository;
    private final UserKycRepository userKycRepository;
    private final ItemRepository itemRepository;
    private final BookingRepository bookingRepository;
    private final BookingDisputeRepository disputeRepository;
    private final ReportRepository reportRepository;
    private final TopupRequestRepository topupRequestRepository;
    private final FinancialReportService financialReportService;

    @Override
    public AdminDashboardResponse getDashboard() {
        OffsetDateTime todayStart = LocalDate.now(REPORTING_ZONE).atStartOfDay(REPORTING_ZONE).toOffsetDateTime();
        OffsetDateTime tomorrowStart = todayStart.plusDays(1);
        OffsetDateTime now = OffsetDateTime.now(REPORTING_ZONE);

        long activeUsers = userRepository.countByAccountStatus(AccountStatus.ACTIVE);
        long suspendedUsers = userRepository.countByAccountStatus(AccountStatus.SUSPENDED);
        long bannedUsers = userRepository.countByAccountStatus(AccountStatus.BANNED);

        long activeVendors = vendorApplicationRepository.countApprovedVendorsByAccountStatus(
                VendorApplicationStatus.APPROVED, AccountStatus.ACTIVE);
        long suspendedVendors = vendorApplicationRepository.countApprovedVendorsByAccountStatus(
                VendorApplicationStatus.APPROVED, AccountStatus.SUSPENDED);
        long bannedVendors = vendorApplicationRepository.countApprovedVendorsByAccountStatus(
                VendorApplicationStatus.APPROVED, AccountStatus.BANNED);
        long pendingApplications = vendorApplicationRepository.countByStatus(VendorApplicationStatus.PENDING);

        long pendingListings = itemRepository.countByDeletedFalseAndApprovalStatus(ItemApprovalStatus.PENDING);
        long openDisputes = disputeRepository.countByStatus("OPEN");

        DashboardFinancialProjection allTime = bookingRepository.sumDashboardFinancials();
        BookingTotalsProjection today = bookingRepository.sumBookingTotals(todayStart, tomorrowStart);

        // Ledger-backed Platform Revenue, reusing the exact same FinancialReportService logic
        // (and therefore the exact same accounting rule) as
        // GET /api/v1/admin/financial-reports/platform-revenue — trailing 12 months is the
        // widest range that service's own validation allows; there is no "all-time" bypass.
        List<PlatformRevenueCurrencySummary> platformRevenue =
                financialReportService.getPlatformRevenueSummary(now.minusYears(1), now).currencies();

        return new AdminDashboardResponse(
                new UserDashboardSummaryResponse(
                        userRepository.count(), activeUsers, suspendedUsers, bannedUsers,
                        userRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(todayStart, tomorrowStart)),
                new VendorDashboardSummaryResponse(
                        activeVendors + suspendedVendors + bannedVendors,
                        activeVendors, suspendedVendors, bannedVendors, pendingApplications),
                new ListingDashboardSummaryResponse(
                        itemRepository.countByDeletedFalse(),
                        itemRepository.countByDeletedFalseAndStatus(ItemStatus.ACTIVE),
                        pendingListings,
                        itemRepository.countByDeletedFalseAndApprovalStatus(ItemApprovalStatus.REJECTED),
                        itemRepository.countCurrentlyFeatured(now)),
                new BookingDashboardSummaryResponse(
                        bookingRepository.count(),
                        bookingRepository.countByStatusIn(ACTIVE_BOOKING_STATUSES),
                        bookingRepository.countByStatus(BookingStatus.COMPLETED),
                        bookingRepository.countByStatus(BookingStatus.CANCELLED),
                        disputeRepository.countDistinctBookingIdByStatus("OPEN")),
                new PendingActionSummaryResponse(
                        pendingApplications,
                        userKycRepository.countByVerificationStatus(KycStatus.PENDING.name()),
                        pendingListings,
                        openDisputes,
                        reportRepository.countByStatusIn(PENDING_REPORT_STATUSES),
                        topupRequestRepository.countByStatus(TopupStatus.PENDING)),
                new FinancialDashboardSummaryResponse(
                        nvl(allTime.getTotalBookingValue()),
                        nvl(today.getTotalBookingValue()),
                        nvl(allTime.getCalculatedCommission()),
                        platformRevenue));
    }

    @Override
    public DashboardTrendResponse getRevenueTrend(LocalDate from, LocalDate to, GroupBy groupBy) {
        ValidatedRange range = validateRange(from, to, groupBy);
        Page<BookingPeriodAggregateProjection> page = groupBy == GroupBy.MONTH
                ? bookingRepository.aggregateBookingTotalsByMonth(
                        range.fromInclusive(), range.toExclusive(), PageRequest.of(0, MAX_TREND_PERIODS))
                : bookingRepository.aggregateBookingTotalsByDay(
                        range.fromInclusive(), range.toExclusive(), PageRequest.of(0, MAX_TREND_PERIODS));

        Map<LocalDate, BigDecimal> values = page.getContent().stream().collect(Collectors.toMap(
                row -> row.getPeriod().toLocalDate(),
                row -> nvl(row.getTotalBookingValue())));

        List<DashboardTrendPointResponse> data = periods(range.from(), range.to(), groupBy).stream()
                .map(period -> new DashboardTrendPointResponse(formatPeriod(period, groupBy),
                        values.getOrDefault(period, BigDecimal.ZERO)))
                .toList();
        return new DashboardTrendResponse(groupBy, range.from(), range.to(), data);
    }

    @Override
    public DashboardCountTrendResponse getBookingTrend(LocalDate from, LocalDate to, GroupBy groupBy) {
        ValidatedRange range = validateRange(from, to, groupBy);
        List<DashboardCountProjection> rows = bookingRepository.countBookingsByPeriod(
                range.fromInclusive(), range.toExclusive(), databaseGroupBy(groupBy));
        return countTrend(range, groupBy, rows);
    }

    @Override
    public DashboardCountTrendResponse getUserGrowth(LocalDate from, LocalDate to, GroupBy groupBy) {
        ValidatedRange range = validateRange(from, to, groupBy);
        List<DashboardCountProjection> rows = userRepository.countRegistrationsByPeriod(
                range.fromInclusive(), range.toExclusive(), databaseGroupBy(groupBy));
        return countTrend(range, groupBy, rows);
    }

    @Override
    public List<RecentDashboardActivityResponse> getRecentActivity(int limit) {
        if (limit < 1 || limit > MAX_RECENT_ACTIVITY_LIMIT) {
            throw new InvalidOperationException("limit must be between 1 and " + MAX_RECENT_ACTIVITY_LIMIT);
        }

        PageRequest recent = PageRequest.of(0, limit);
        List<RecentDashboardActivityResponse> activities = new ArrayList<>();

        userRepository.findAllByOrderByCreatedAtDesc(recent).forEach(user -> activities.add(activity(
                DashboardActivityType.USER_REGISTRATION, user.getId(), "New user registration",
                user.getAccountStatus().name(), user.getCreatedAt())));
        vendorApplicationRepository.findAllByOrderByCreatedAtDesc(recent).forEach(application -> activities.add(activity(
                DashboardActivityType.VENDOR_APPLICATION, application.getId(), "Vendor application submitted",
                application.getStatus().name(), application.getCreatedAt())));
        userKycRepository.findAllByOrderByCreatedAtDesc(recent).forEach(kyc -> activities.add(activity(
                DashboardActivityType.KYC_SUBMISSION, kyc.getId(), "KYC submission received",
                kyc.getVerificationStatus(), kyc.getCreatedAt())));
        itemRepository.findAllByDeletedFalseOrderByCreatedAtDesc(recent).forEach(item -> activities.add(activity(
                DashboardActivityType.ITEM_SUBMISSION, item.getId(), "Item submitted",
                item.getApprovalStatus().name(), item.getCreatedAt())));
        bookingRepository.findAllByOrderByCreatedAtDesc(recent).forEach(booking -> activities.add(activity(
                DashboardActivityType.BOOKING, booking.getId(), "Booking created",
                booking.getStatus().name(), booking.getCreatedAt())));
        disputeRepository.findAllByOrderByCreatedAtDesc(recent).forEach(dispute -> activities.add(activity(
                DashboardActivityType.DISPUTE, dispute.getId(), "Booking dispute opened",
                dispute.getStatus(), OffsetDateTime.ofInstant(dispute.getCreatedAt(), REPORTING_ZONE))));
        reportRepository.findAllByOrderByCreatedAtDesc(recent).forEach(report -> activities.add(activity(
                DashboardActivityType.REPORT, report.getId(), "User report submitted",
                report.getStatus().name(), report.getCreatedAt())));
        topupRequestRepository.findAllByOrderByCreatedAtDesc(recent).forEach(topup -> activities.add(activity(
                DashboardActivityType.TOP_UP_REQUEST, topup.getId(), "Top-up request created",
                topup.getStatus().name(), topup.getCreatedAt())));

        return activities.stream()
                .filter(activity -> activity.createdAt() != null)
                .sorted(Comparator.comparing(RecentDashboardActivityResponse::createdAt).reversed())
                .limit(limit)
                .toList();
    }

    private DashboardCountTrendResponse countTrend(
            ValidatedRange range, GroupBy groupBy, List<DashboardCountProjection> rows) {
        Map<LocalDate, Long> values = rows.stream().collect(Collectors.toMap(
                row -> row.getPeriod().toLocalDate(), row -> nvl(row.getValue())));
        List<DashboardCountTrendPointResponse> data = periods(range.from(), range.to(), groupBy).stream()
                .map(period -> new DashboardCountTrendPointResponse(
                        formatPeriod(period, groupBy), values.getOrDefault(period, 0L)))
                .toList();
        return new DashboardCountTrendResponse(groupBy, range.from(), range.to(), data);
    }

    private ValidatedRange validateRange(LocalDate from, LocalDate to, GroupBy groupBy) {
        if (from == null) {
            throw new InvalidOperationException("from date is required");
        }
        if (groupBy == null) {
            throw new InvalidOperationException("groupBy is required");
        }
        LocalDate effectiveTo = to != null ? to : LocalDate.now(REPORTING_ZONE);
        if (effectiveTo.isBefore(from)) {
            throw new InvalidOperationException("from date must not be after to date");
        }
        if (effectiveTo.isAfter(from.plusYears(MAX_TREND_RANGE_YEARS))) {
            throw new InvalidOperationException("Dashboard trend date range cannot exceed " + MAX_TREND_RANGE_YEARS + " year");
        }
        return new ValidatedRange(
                from, effectiveTo,
                from.atStartOfDay(REPORTING_ZONE).toOffsetDateTime(),
                effectiveTo.plusDays(1).atStartOfDay(REPORTING_ZONE).toOffsetDateTime());
    }

    private List<LocalDate> periods(LocalDate from, LocalDate to, GroupBy groupBy) {
        LocalDate first = groupBy == GroupBy.MONTH ? from.withDayOfMonth(1) : from;
        LocalDate last = groupBy == GroupBy.MONTH ? to.withDayOfMonth(1) : to;
        List<LocalDate> periods = new ArrayList<>();
        for (LocalDate current = first; !current.isAfter(last);
             current = groupBy == GroupBy.MONTH ? current.plusMonths(1) : current.plusDays(1)) {
            periods.add(current);
        }
        return periods;
    }

    private String formatPeriod(LocalDate period, GroupBy groupBy) {
        return groupBy == GroupBy.MONTH
                ? period.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                : period.toString();
    }

    private String databaseGroupBy(GroupBy groupBy) {
        return groupBy == GroupBy.MONTH ? "month" : "day";
    }

    private RecentDashboardActivityResponse activity(
            DashboardActivityType type, Object referenceId, String title, String status, OffsetDateTime createdAt) {
        return new RecentDashboardActivityResponse(type, String.valueOf(referenceId), title, status, createdAt);
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static long nvl(Long value) {
        return value != null ? value : 0L;
    }

    private record ValidatedRange(
            LocalDate from, LocalDate to, OffsetDateTime fromInclusive, OffsetDateTime toExclusive) {}
}
