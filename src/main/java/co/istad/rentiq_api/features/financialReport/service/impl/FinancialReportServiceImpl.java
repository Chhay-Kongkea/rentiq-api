package co.istad.rentiq_api.features.financialReport.service.impl;

import co.istad.rentiq_api.common.exception.InvalidOperationException;
import co.istad.rentiq_api.features.bookings.enums.PaymentStatus;
import co.istad.rentiq_api.features.bookings.repository.BookingRepository;
import co.istad.rentiq_api.features.financialReport.dto.GroupBy;
import co.istad.rentiq_api.features.financialReport.dto.projection.BookingPeriodAggregateProjection;
import co.istad.rentiq_api.features.financialReport.dto.projection.BookingTotalsProjection;
import co.istad.rentiq_api.features.financialReport.dto.projection.VendorEarningsPeriodProjection;
import co.istad.rentiq_api.features.financialReport.dto.projection.VendorEarningsTotalsProjection;
import co.istad.rentiq_api.features.financialReport.dto.projection.WalletTransactionPeriodProjection;
import co.istad.rentiq_api.features.financialReport.dto.response.CommissionTimeSeriesResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.CommissionTimeSeriesRow;
import co.istad.rentiq_api.features.financialReport.dto.response.RevenuePeriodRow;
import co.istad.rentiq_api.features.financialReport.dto.response.RevenueReportResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.TransactionPeriodRow;
import co.istad.rentiq_api.features.financialReport.dto.response.TransactionReportResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.TransactionTypeSummaryRow;
import co.istad.rentiq_api.features.financialReport.dto.response.VendorEarningsPeriodRow;
import co.istad.rentiq_api.features.financialReport.dto.response.VendorEarningsReportResponse;
import co.istad.rentiq_api.features.financialReport.service.FinancialReportService;
import co.istad.rentiq_api.features.financialReport.service.ReportPagingLimits;
import co.istad.rentiq_api.features.wallet.enums.TransactionDirection;
import co.istad.rentiq_api.features.wallet.enums.TransactionType;
import co.istad.rentiq_api.features.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class FinancialReportServiceImpl implements FinancialReportService {

    private static final int MAX_REPORT_RANGE_YEARS = 1;

    private final BookingRepository bookingRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final FinancialReportExportGenerator exportGenerator;

    private record ValidatedRange(LocalDate from, LocalDate to, OffsetDateTime fromInclusive, OffsetDateTime toExclusive) {}

    private ValidatedRange validateRange(LocalDate from, LocalDate to) {
        if (from == null) {
            throw new InvalidOperationException("from date is required");
        }

        LocalDate effectiveTo = to != null ? to : LocalDate.now();

        if (effectiveTo.isBefore(from)) {
            throw new InvalidOperationException("from date must not be after to date");
        }

        if (effectiveTo.isAfter(from.plusYears(MAX_REPORT_RANGE_YEARS))) {
            throw new InvalidOperationException("Report date range cannot exceed " + MAX_REPORT_RANGE_YEARS + " year");
        }

        OffsetDateTime fromInclusive = from.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime toExclusive = effectiveTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

        return new ValidatedRange(from, effectiveTo, fromInclusive, toExclusive);
    }


    private Pageable capPageSize(Pageable pageable, int maxSize) {
        if (pageable.getPageSize() > maxSize) {
            throw new InvalidOperationException("size must not exceed " + maxSize);
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static long nvl(Long value) {
        return value != null ? value : 0L;
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenueReport(LocalDate from, LocalDate to, GroupBy groupBy, Pageable pageable) {
        ValidatedRange range = validateRange(from, to);
        Pageable capped = capPageSize(pageable, ReportPagingLimits.MAX_PERIOD_PAGE_SIZE);

        BookingTotalsProjection totals = bookingRepository.sumRevenueAndCommissionTotals(
                PaymentStatus.RELEASED_TO_VENDOR, range.fromInclusive(), range.toExclusive());

        Page<BookingPeriodAggregateProjection> page = fetchBookingPeriodPage(range, groupBy, capped);

        Page<RevenuePeriodRow> rows = page.map(row -> new RevenuePeriodRow(
                row.getPeriod().toLocalDate(), nvl(row.getTotalRevenue()), nvl(row.getBookingCount())));

        return new RevenueReportResponse(
                range.from(), range.to(), groupBy,
                nvl(totals.getTotalRevenue()), nvl(totals.getBookingCount()), rows);
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionTimeSeriesResponse getCommissionReport(LocalDate from, LocalDate to, GroupBy groupBy, Pageable pageable) {
        ValidatedRange range = validateRange(from, to);
        Pageable capped = capPageSize(pageable, ReportPagingLimits.MAX_PERIOD_PAGE_SIZE);


        BookingTotalsProjection totals = bookingRepository.sumRevenueAndCommissionTotals(
                PaymentStatus.RELEASED_TO_VENDOR, range.fromInclusive(), range.toExclusive());

        Page<BookingPeriodAggregateProjection> page = fetchBookingPeriodPage(range, groupBy, capped);

        Page<CommissionTimeSeriesRow> rows = page.map(row -> new CommissionTimeSeriesRow(
                row.getPeriod().toLocalDate(), nvl(row.getTotalCommission()), nvl(row.getBookingCount())));

        return new CommissionTimeSeriesResponse(
                range.from(), range.to(), groupBy,
                nvl(totals.getTotalCommission()), nvl(totals.getBookingCount()), rows);
    }

    private Page<BookingPeriodAggregateProjection> fetchBookingPeriodPage(ValidatedRange range, GroupBy groupBy, Pageable pageable) {
        return groupBy == GroupBy.MONTH
                ? bookingRepository.aggregateRevenueAndCommissionByMonth(
                        PaymentStatus.RELEASED_TO_VENDOR.name(), range.fromInclusive(), range.toExclusive(), pageable)
                : bookingRepository.aggregateRevenueAndCommissionByDay(
                        PaymentStatus.RELEASED_TO_VENDOR.name(), range.fromInclusive(), range.toExclusive(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionReportResponse getTransactionReport(LocalDate from, LocalDate to, GroupBy groupBy, Pageable pageable) {
        ValidatedRange range = validateRange(from, to);
        Pageable capped = capPageSize(pageable, ReportPagingLimits.MAX_TRANSACTION_PAGE_SIZE);

        java.util.List<TransactionTypeSummaryRow> summary = walletTransactionRepository
                .sumTransactionTotalsByTypeAndDirection(range.fromInclusive(), range.toExclusive())
                .stream()
                .map(row -> new TransactionTypeSummaryRow(
                        TransactionType.valueOf(row.getTransactionType()),
                        TransactionDirection.valueOf(row.getDirection()),
                        nvl(row.getTotalAmount()),
                        nvl(row.getTransactionCount())))
                .toList();

        Page<WalletTransactionPeriodProjection> page = groupBy == GroupBy.MONTH
                ? walletTransactionRepository.aggregateTransactionsByMonth(range.fromInclusive(), range.toExclusive(), capped)
                : walletTransactionRepository.aggregateTransactionsByDay(range.fromInclusive(), range.toExclusive(), capped);

        Page<TransactionPeriodRow> rows = page.map(row -> new TransactionPeriodRow(
                row.getPeriod().toLocalDate(),
                TransactionType.valueOf(row.getTransactionType()),
                TransactionDirection.valueOf(row.getDirection()),
                nvl(row.getTotalAmount()),
                nvl(row.getTransactionCount())));

        return new TransactionReportResponse(range.from(), range.to(), groupBy, summary, rows);
    }

    @Override
    @Transactional(readOnly = true)
    public VendorEarningsReportResponse getVendorEarningsReport(String ownerId, LocalDate from, LocalDate to, GroupBy groupBy, Pageable pageable) {
        ValidatedRange range = validateRange(from, to);
        Pageable capped = capPageSize(pageable, ReportPagingLimits.MAX_PERIOD_PAGE_SIZE);

        VendorEarningsTotalsProjection totals = walletTransactionRepository
                .sumVendorEarningsTotalsForOwner(ownerId, range.fromInclusive(), range.toExclusive());

        Page<VendorEarningsPeriodProjection> page = groupBy == GroupBy.MONTH
                ? walletTransactionRepository.aggregateVendorEarningsByMonth(ownerId, range.fromInclusive(), range.toExclusive(), capped)
                : walletTransactionRepository.aggregateVendorEarningsByDay(ownerId, range.fromInclusive(), range.toExclusive(), capped);

        Page<VendorEarningsPeriodRow> rows = page.map(row -> new VendorEarningsPeriodRow(
                row.getPeriod().toLocalDate(), nvl(row.getTotalEarnings()), nvl(row.getTransactionCount())));

        return new VendorEarningsReportResponse(
                range.from(), range.to(), groupBy,
                nvl(totals.getTotalEarnings()), nvl(totals.getTransactionCount()), rows);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportRevenuePdf(LocalDate from, LocalDate to, GroupBy groupBy) {
        return exportGenerator.generateRevenuePdf(getRevenueReport(from, to, groupBy, fullRangePageable()));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportRevenueXlsx(LocalDate from, LocalDate to, GroupBy groupBy) {
        return exportGenerator.generateRevenueXlsx(getRevenueReport(from, to, groupBy, fullRangePageable()));
    }


    private Pageable fullRangePageable() {
        return PageRequest.of(0, ReportPagingLimits.MAX_PERIOD_PAGE_SIZE);
    }
}
