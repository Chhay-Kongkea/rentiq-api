package co.istad.rentiq_api.features.financialReport.service.impl;

import co.istad.rentiq_api.common.exception.InvalidOperationException;
import co.istad.rentiq_api.features.bookings.enums.BookingStatus;
import co.istad.rentiq_api.features.bookings.repository.BookingRepository;
import co.istad.rentiq_api.features.financialReport.dto.GroupBy;
import co.istad.rentiq_api.features.financialReport.dto.projection.BookingPeriodAggregateProjection;
import co.istad.rentiq_api.features.financialReport.dto.projection.BookingTotalsProjection;
import co.istad.rentiq_api.features.financialReport.dto.projection.PlatformRevenueAggregateProjection;
import co.istad.rentiq_api.features.financialReport.dto.projection.PlatformRevenueTrendAggregateProjection;
import co.istad.rentiq_api.features.financialReport.dto.projection.VendorBookingValueCurrencyProjection;
import co.istad.rentiq_api.features.financialReport.dto.projection.VendorBookingValueTrendProjection;
import co.istad.rentiq_api.features.financialReport.dto.projection.WalletTransactionPeriodProjection;
import co.istad.rentiq_api.features.financialReport.dto.response.CommissionTimeSeriesResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.CommissionTimeSeriesRow;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueBreakdownResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueCurrencyBreakdown;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueCurrencySummary;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueCurrencyTrend;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueSourceBreakdown;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueSummaryResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueTrendPoint;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueTrendResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.RevenuePeriodRow;
import co.istad.rentiq_api.features.financialReport.dto.response.RevenueReportResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.TransactionPeriodRow;
import co.istad.rentiq_api.features.financialReport.dto.response.TransactionReportResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.TransactionTypeSummaryRow;
import co.istad.rentiq_api.features.financialReport.dto.response.VendorBookingValueCurrencySummary;
import co.istad.rentiq_api.features.financialReport.dto.response.VendorBookingValueCurrencyTrend;
import co.istad.rentiq_api.features.financialReport.dto.response.VendorBookingValuePeriodPoint;
import co.istad.rentiq_api.features.financialReport.dto.response.VendorBookingValueReportResponse;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        BookingTotalsProjection totals = bookingRepository.sumBookingTotals(range.fromInclusive(), range.toExclusive());

        Page<BookingPeriodAggregateProjection> page = fetchBookingPeriodPage(range, groupBy, capped);

        Page<RevenuePeriodRow> rows = page.map(row -> new RevenuePeriodRow(
                row.getPeriod().toLocalDate(), nvl(row.getTotalBookingValue()), nvl(row.getBookingCount())));

        return new RevenueReportResponse(
                range.from(), range.to(), groupBy,
                nvl(totals.getTotalBookingValue()), nvl(totals.getBookingCount()), rows);
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionTimeSeriesResponse getCommissionReport(LocalDate from, LocalDate to, GroupBy groupBy, Pageable pageable) {
        ValidatedRange range = validateRange(from, to);
        Pageable capped = capPageSize(pageable, ReportPagingLimits.MAX_PERIOD_PAGE_SIZE);


        BookingTotalsProjection totals = bookingRepository.sumBookingTotals(range.fromInclusive(), range.toExclusive());

        Page<BookingPeriodAggregateProjection> page = fetchBookingPeriodPage(range, groupBy, capped);

        Page<CommissionTimeSeriesRow> rows = page.map(row -> new CommissionTimeSeriesRow(
                row.getPeriod().toLocalDate(), nvl(row.getTotalCommission()), nvl(row.getBookingCount())));

        return new CommissionTimeSeriesResponse(
                range.from(), range.to(), groupBy,
                nvl(totals.getTotalCommission()), nvl(totals.getBookingCount()), rows);
    }

    private Page<BookingPeriodAggregateProjection> fetchBookingPeriodPage(ValidatedRange range, GroupBy groupBy, Pageable pageable) {
        return groupBy == GroupBy.MONTH
                ? bookingRepository.aggregateBookingTotalsByMonth(range.fromInclusive(), range.toExclusive(), pageable)
                : bookingRepository.aggregateBookingTotalsByDay(range.fromInclusive(), range.toExclusive(), pageable);
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

    // ---------------------------------------------------------------
    // Vendor Booking Value report — a single Vendor's own COMPLETED bookings (marketplace
    // rental GMV), per currency. Source of truth is BookingRepository, using the exact same
    // field (subtotal) and status (COMPLETED) as VendorPerformanceServiceImpl.completedBookingValue
    // — one business definition, never two. NEVER WalletTransaction: rental payment is P2P and
    // never touches Rentiq, so no booking-linked wallet transaction is ever created. USD and
    // KHR are always reported separately, never combined, no conversion.
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public VendorBookingValueReportResponse getVendorBookingValueReport(
            String ownerId, LocalDate from, LocalDate to, GroupBy groupBy) {
        ValidatedRange range = validateRange(from, to);

        List<VendorBookingValueCurrencyProjection> totalsRows = bookingRepository
                .sumCompletedBookingValueByOwnerAndCurrency(
                        ownerId, BookingStatus.COMPLETED, range.fromInclusive(), range.toExclusive());
        List<VendorBookingValueCurrencySummary> currencies = buildVendorBookingValueSummaries(totalsRows);

        List<VendorBookingValueTrendProjection> trendRows = groupBy == GroupBy.MONTH
                ? bookingRepository.aggregateCompletedBookingValueByOwnerAndMonth(ownerId, range.fromInclusive(), range.toExclusive())
                : bookingRepository.aggregateCompletedBookingValueByOwnerAndDay(ownerId, range.fromInclusive(), range.toExclusive());
        List<VendorBookingValueCurrencyTrend> trend =
                buildVendorBookingValueTrend(trendRows, range.fromInclusive(), range.toExclusive(), groupBy);

        return new VendorBookingValueReportResponse(range.from(), range.to(), groupBy, currencies, trend);
    }

    private List<VendorBookingValueCurrencySummary> buildVendorBookingValueSummaries(
            List<VendorBookingValueCurrencyProjection> rows) {
        Map<String, BigDecimal> valueByCurrency = new LinkedHashMap<>();
        Map<String, Long> countByCurrency = new LinkedHashMap<>();
        for (String currency : SUPPORTED_REVENUE_CURRENCIES) {
            valueByCurrency.put(currency, BigDecimal.ZERO);
            countByCurrency.put(currency, 0L);
        }

        for (VendorBookingValueCurrencyProjection row : rows) {
            String currency = row.getCurrency();
            if (!valueByCurrency.containsKey(currency)) {
                continue;
            }
            valueByCurrency.put(currency, nvl(row.getTotalBookingValue()));
            countByCurrency.put(currency, nvl(row.getBookingCount()));
        }

        List<VendorBookingValueCurrencySummary> summaries = new ArrayList<>();
        for (String currency : SUPPORTED_REVENUE_CURRENCIES) {
            BigDecimal value = valueByCurrency.get(currency);
            long count = countByCurrency.get(currency);
            BigDecimal average = count == 0
                    ? BigDecimal.ZERO
                    : value.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
            summaries.add(new VendorBookingValueCurrencySummary(currency, value, count, average));
        }
        return summaries;
    }

    private List<VendorBookingValueCurrencyTrend> buildVendorBookingValueTrend(
            List<VendorBookingValueTrendProjection> rows, OffsetDateTime from, OffsetDateTime to, GroupBy groupBy) {
        Map<String, Map<LocalDate, VendorBookingValueBucket>> buckets = new LinkedHashMap<>();
        for (VendorBookingValueTrendProjection row : rows) {
            String currency = row.getCurrency();
            if (!SUPPORTED_REVENUE_CURRENCIES.contains(currency)) {
                continue;
            }
            LocalDate period = row.getPeriod().toLocalDate();
            VendorBookingValueBucket bucket = buckets
                    .computeIfAbsent(currency, key -> new LinkedHashMap<>())
                    .computeIfAbsent(period, key -> new VendorBookingValueBucket());
            bucket.value = nvl(row.getTotalBookingValue());
            bucket.count = nvl(row.getBookingCount());
        }

        List<LocalDate> periods = generatePeriods(from, to, groupBy);

        List<VendorBookingValueCurrencyTrend> currencies = new ArrayList<>();
        for (String currency : SUPPORTED_REVENUE_CURRENCIES) {
            Map<LocalDate, VendorBookingValueBucket> currencyBuckets = buckets.getOrDefault(currency, Map.of());
            List<VendorBookingValuePeriodPoint> points = new ArrayList<>();
            for (LocalDate period : periods) {
                VendorBookingValueBucket bucket = currencyBuckets.getOrDefault(period, new VendorBookingValueBucket());
                points.add(new VendorBookingValuePeriodPoint(formatPeriod(period, groupBy), bucket.value, bucket.count));
            }
            currencies.add(new VendorBookingValueCurrencyTrend(currency, points));
        }
        return currencies;
    }

    private static final class VendorBookingValueBucket {
        private BigDecimal value = BigDecimal.ZERO;
        private long count = 0L;
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

    // ---------------------------------------------------------------
    // Platform Revenue — source of truth is WalletTransaction PROMOTION/ADVERTISEMENT OUT
    // rows only (see WalletTransactionRepository.aggregatePlatformRevenue*). Never booking
    // subtotal, never TOP_UP, never ADMIN_ADJUSTMENT, never a domain-object price/quote.
    // Grouping/zero-fill is done here in Java over the small, already-aggregated result set
    // the database returns — never over raw ledger rows.
    // ---------------------------------------------------------------

    private static final List<String> SUPPORTED_REVENUE_CURRENCIES = List.of("USD", "KHR");
    private static final DateTimeFormatter MONTH_PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private void validateRevenueRange(OffsetDateTime from, OffsetDateTime to) {
        if (from == null) {
            throw new InvalidOperationException("from is required");
        }
        if (to == null) {
            throw new InvalidOperationException("to is required");
        }
        if (!from.isBefore(to)) {
            throw new InvalidOperationException("from must be strictly before to");
        }
        if (to.isAfter(from.plusYears(MAX_REPORT_RANGE_YEARS))) {
            throw new InvalidOperationException("Report date range cannot exceed " + MAX_REPORT_RANGE_YEARS + " year");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformRevenueSummaryResponse getPlatformRevenueSummary(OffsetDateTime from, OffsetDateTime to) {
        validateRevenueRange(from, to);

        List<PlatformRevenueAggregateProjection> rows = walletTransactionRepository.aggregatePlatformRevenue(from, to);
        List<PlatformRevenueCurrencySummary> currencies = buildCurrencySummaries(rows);

        return new PlatformRevenueSummaryResponse(from, to, currencies);
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformRevenueBreakdownResponse getPlatformRevenueBreakdown(OffsetDateTime from, OffsetDateTime to) {
        validateRevenueRange(from, to);

        List<PlatformRevenueAggregateProjection> rows = walletTransactionRepository.aggregatePlatformRevenue(from, to);
        List<PlatformRevenueCurrencySummary> summaries = buildCurrencySummaries(rows);

        List<PlatformRevenueCurrencyBreakdown> currencies = summaries.stream()
                .map(summary -> new PlatformRevenueCurrencyBreakdown(
                        summary.currency(),
                        summary.totalRevenue(),
                        List.of(
                                sourceBreakdown("PROMOTION", summary.promotionRevenue(), summary.promotionTransactions(), summary.totalRevenue()),
                                sourceBreakdown("ADVERTISEMENT", summary.advertisementRevenue(), summary.advertisementTransactions(), summary.totalRevenue())
                        )))
                .toList();

        return new PlatformRevenueBreakdownResponse(from, to, currencies);
    }

    private PlatformRevenueSourceBreakdown sourceBreakdown(String source, BigDecimal revenue, long count, BigDecimal currencyTotal) {
        BigDecimal percentage = currencyTotal.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : revenue.multiply(BigDecimal.valueOf(100)).divide(currencyTotal, 2, RoundingMode.HALF_UP);
        return new PlatformRevenueSourceBreakdown(source, revenue, count, percentage);
    }

    private List<PlatformRevenueCurrencySummary> buildCurrencySummaries(List<PlatformRevenueAggregateProjection> rows) {
        Map<String, BigDecimal> promotionRevenue = new LinkedHashMap<>();
        Map<String, BigDecimal> advertisementRevenue = new LinkedHashMap<>();
        Map<String, Long> promotionCount = new LinkedHashMap<>();
        Map<String, Long> advertisementCount = new LinkedHashMap<>();
        for (String currency : SUPPORTED_REVENUE_CURRENCIES) {
            promotionRevenue.put(currency, BigDecimal.ZERO);
            advertisementRevenue.put(currency, BigDecimal.ZERO);
            promotionCount.put(currency, 0L);
            advertisementCount.put(currency, 0L);
        }

        for (PlatformRevenueAggregateProjection row : rows) {
            String currency = row.getCurrency();
            if (!promotionRevenue.containsKey(currency)) {
                continue;
            }
            BigDecimal amount = nvl(row.getTotalAmount());
            long count = nvl(row.getTransactionCount());
            if (TransactionType.PROMOTION.name().equals(row.getTransactionType())) {
                promotionRevenue.put(currency, amount);
                promotionCount.put(currency, count);
            } else if (TransactionType.ADVERTISEMENT.name().equals(row.getTransactionType())) {
                advertisementRevenue.put(currency, amount);
                advertisementCount.put(currency, count);
            }
        }

        List<PlatformRevenueCurrencySummary> summaries = new ArrayList<>();
        for (String currency : SUPPORTED_REVENUE_CURRENCIES) {
            BigDecimal promotion = promotionRevenue.get(currency);
            BigDecimal advertisement = advertisementRevenue.get(currency);
            long promotionTx = promotionCount.get(currency);
            long advertisementTx = advertisementCount.get(currency);
            summaries.add(new PlatformRevenueCurrencySummary(
                    currency, promotion.add(advertisement), promotion, advertisement,
                    promotionTx + advertisementTx, promotionTx, advertisementTx));
        }
        return summaries;
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformRevenueTrendResponse getPlatformRevenueTrend(OffsetDateTime from, OffsetDateTime to, GroupBy groupBy) {
        validateRevenueRange(from, to);

        List<PlatformRevenueTrendAggregateProjection> rows = groupBy == GroupBy.MONTH
                ? walletTransactionRepository.aggregatePlatformRevenueByMonth(from, to)
                : walletTransactionRepository.aggregatePlatformRevenueByDay(from, to);

        // currency -> period -> bucket
        Map<String, Map<LocalDate, RevenueBucket>> buckets = new LinkedHashMap<>();
        for (PlatformRevenueTrendAggregateProjection row : rows) {
            String currency = row.getCurrency();
            if (!SUPPORTED_REVENUE_CURRENCIES.contains(currency)) {
                continue;
            }
            LocalDate period = row.getPeriod().toLocalDate();
            RevenueBucket bucket = buckets
                    .computeIfAbsent(currency, key -> new LinkedHashMap<>())
                    .computeIfAbsent(period, key -> new RevenueBucket());

            BigDecimal amount = nvl(row.getTotalAmount());
            long count = nvl(row.getTransactionCount());
            if (TransactionType.PROMOTION.name().equals(row.getTransactionType())) {
                bucket.promotionRevenue = amount;
                bucket.promotionCount = count;
            } else if (TransactionType.ADVERTISEMENT.name().equals(row.getTransactionType())) {
                bucket.advertisementRevenue = amount;
                bucket.advertisementCount = count;
            }
        }

        List<LocalDate> periods = generatePeriods(from, to, groupBy);

        List<PlatformRevenueCurrencyTrend> currencies = new ArrayList<>();
        for (String currency : SUPPORTED_REVENUE_CURRENCIES) {
            Map<LocalDate, RevenueBucket> currencyBuckets = buckets.getOrDefault(currency, Map.of());
            List<PlatformRevenueTrendPoint> points = new ArrayList<>();
            for (LocalDate period : periods) {
                RevenueBucket bucket = currencyBuckets.getOrDefault(period, new RevenueBucket());
                points.add(new PlatformRevenueTrendPoint(
                        formatPeriod(period, groupBy),
                        bucket.promotionRevenue.add(bucket.advertisementRevenue),
                        bucket.promotionRevenue,
                        bucket.advertisementRevenue,
                        bucket.promotionCount + bucket.advertisementCount));
            }
            currencies.add(new PlatformRevenueCurrencyTrend(currency, points));
        }

        return new PlatformRevenueTrendResponse(from, to, groupBy, currencies);
    }

    /**
     * Every possible period bucket inside [from, to), grouped by UTC — matching the database
     * query's own {@code date_trunc(..., wt.created_at at time zone 'UTC')}. The last instant
     * strictly before {@code to} determines the final bucket, so a `to` that falls mid-period
     * still yields a (correctly partial) final bucket instead of silently omitting it.
     */
    private List<LocalDate> generatePeriods(OffsetDateTime from, OffsetDateTime to, GroupBy groupBy) {
        LocalDate startDay = from.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
        LocalDate endDayInclusive = to.withOffsetSameInstant(ZoneOffset.UTC).minusNanos(1).toLocalDate();

        List<LocalDate> periods = new ArrayList<>();
        if (groupBy == GroupBy.MONTH) {
            LocalDate month = startDay.withDayOfMonth(1);
            LocalDate lastMonth = endDayInclusive.withDayOfMonth(1);
            while (!month.isAfter(lastMonth)) {
                periods.add(month);
                month = month.plusMonths(1);
            }
        } else {
            LocalDate day = startDay;
            while (!day.isAfter(endDayInclusive)) {
                periods.add(day);
                day = day.plusDays(1);
            }
        }
        return periods;
    }

    private String formatPeriod(LocalDate period, GroupBy groupBy) {
        return groupBy == GroupBy.MONTH ? period.format(MONTH_PERIOD_FORMAT) : period.toString();
    }

    private static final class RevenueBucket {
        private BigDecimal promotionRevenue = BigDecimal.ZERO;
        private long promotionCount = 0L;
        private BigDecimal advertisementRevenue = BigDecimal.ZERO;
        private long advertisementCount = 0L;
    }
}
