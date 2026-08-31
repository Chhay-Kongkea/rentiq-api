package co.istad.rentiq_api.features.financialReport.service.impl;

import co.istad.rentiq_api.common.config.props.KeycloakAdminClientProps;
import co.istad.rentiq_api.common.exception.InvalidOperationException;
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
import co.istad.rentiq_api.features.vendorPerformance.service.impl.VendorPerformanceServiceImpl;
import co.istad.rentiq_api.features.financialReport.dto.GroupBy;
import co.istad.rentiq_api.features.financialReport.dto.projection.BookingTotalsProjection;
import co.istad.rentiq_api.features.financialReport.dto.projection.PlatformRevenueAggregateProjection;
import co.istad.rentiq_api.features.financialReport.dto.projection.PlatformRevenueTrendAggregateProjection;
import co.istad.rentiq_api.features.financialReport.dto.projection.VendorBookingValueCurrencyProjection;
import co.istad.rentiq_api.features.financialReport.dto.projection.VendorBookingValueTrendProjection;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueBreakdownResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueCurrencyBreakdown;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueCurrencySummary;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueCurrencyTrend;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueSourceBreakdown;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueSummaryResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueTrendPoint;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueTrendResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.RevenueReportResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.VendorBookingValueCurrencySummary;
import co.istad.rentiq_api.features.financialReport.dto.response.VendorBookingValueCurrencyTrend;
import co.istad.rentiq_api.features.financialReport.dto.response.VendorBookingValueReportResponse;
import co.istad.rentiq_api.features.wallet.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialReportServiceImplTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private WalletTransactionRepository walletTransactionRepository;
    @Mock private FinancialReportExportGenerator exportGenerator;

    private FinancialReportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FinancialReportServiceImpl(bookingRepository, walletTransactionRepository, exportGenerator);
    }

    private record FakeAggregateRow(String currency, String transactionType, BigDecimal totalAmount, Long transactionCount)
            implements PlatformRevenueAggregateProjection {
        @Override public String getCurrency() { return currency; }
        @Override public String getTransactionType() { return transactionType; }
        @Override public BigDecimal getTotalAmount() { return totalAmount; }
        @Override public Long getTransactionCount() { return transactionCount; }
    }

    private record FakeTrendRow(Date period, String currency, String transactionType, BigDecimal totalAmount, Long transactionCount)
            implements PlatformRevenueTrendAggregateProjection {
        @Override public Date getPeriod() { return period; }
        @Override public String getCurrency() { return currency; }
        @Override public String getTransactionType() { return transactionType; }
        @Override public BigDecimal getTotalAmount() { return totalAmount; }
        @Override public Long getTransactionCount() { return transactionCount; }
    }

    private record FakeVendorCurrencyRow(String currency, BigDecimal totalBookingValue, Long bookingCount)
            implements VendorBookingValueCurrencyProjection {
        @Override public String getCurrency() { return currency; }
        @Override public BigDecimal getTotalBookingValue() { return totalBookingValue; }
        @Override public Long getBookingCount() { return bookingCount; }
    }

    private record FakeVendorTrendRow(Date period, String currency, BigDecimal totalBookingValue, Long bookingCount)
            implements VendorBookingValueTrendProjection {
        @Override public Date getPeriod() { return period; }
        @Override public String getCurrency() { return currency; }
        @Override public BigDecimal getTotalBookingValue() { return totalBookingValue; }
        @Override public Long getBookingCount() { return bookingCount; }
    }

    // ---------------------------------------------------------------
    // Booking GMV vs. Platform Revenue — a concrete worked example (backend audit FIN-001/
    // FIN-002/FIN-003). These are two entirely independent numbers sourced from different
    // tables: booking value never counts toward Platform Revenue, and Platform Revenue never
    // counts toward booking value.
    // ---------------------------------------------------------------

    @Test
    void bookingGmvAndPlatformRevenue_areIndependentNumbers_fromDifferentSources() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);
        Pageable pageable = PageRequest.of(0, 20);

        // $10,000 worth of rentals were arranged through Rentiq this month (marketplace GMV) —
        // renters paid vendors directly, so none of this money ever touched Rentiq.
        BookingTotalsProjection bookingTotals = mock(BookingTotalsProjection.class);
        when(bookingTotals.getTotalBookingValue()).thenReturn(new BigDecimal("10000.00"));
        when(bookingTotals.getBookingCount()).thenReturn(50L);
        when(bookingRepository.sumBookingTotals(any(), any())).thenReturn(bookingTotals);
        when(bookingRepository.aggregateBookingTotalsByDay(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        RevenueReportResponse revenueReport = service.getRevenueReport(from, to, GroupBy.DAY, pageable);

        // Meanwhile, actual Platform Revenue this month was only $350 — entirely from
        // Promotion/Advertisement wallet charges, unrelated to the $10,000 of booking GMV above.
        OffsetDateTime platformFrom = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        OffsetDateTime platformTo = OffsetDateTime.parse("2026-09-01T00:00:00Z");
        when(walletTransactionRepository.aggregatePlatformRevenue(platformFrom, platformTo)).thenReturn(List.of(
                new FakeAggregateRow("USD", "PROMOTION", new BigDecimal("150.00"), 5L),
                new FakeAggregateRow("USD", "ADVERTISEMENT", new BigDecimal("200.00"), 3L)));

        PlatformRevenueSummaryResponse platformRevenue = service.getPlatformRevenueSummary(platformFrom, platformTo);

        assertThat(revenueReport.totalBookingValue()).isEqualByComparingTo("10000.00");
        BigDecimal platformTotal = findCurrency(platformRevenue.currencies(), "USD").totalRevenue();
        assertThat(platformTotal).isEqualByComparingTo("350.00");
        // The two figures are wildly different and neither is derived from the other — proof
        // that booking GMV was never folded into, or confused with, Platform Revenue.
        assertThat(revenueReport.totalBookingValue()).isNotEqualByComparingTo(platformTotal);
    }

    // ---------------------------------------------------------------
    // Summary — revenue source, exclusions, multi-currency
    // ---------------------------------------------------------------

    @Test
    void getPlatformRevenueSummary_sumsPromotionAndAdvertisement_perCurrency() {
        OffsetDateTime from = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-09-01T00:00:00Z");
        when(walletTransactionRepository.aggregatePlatformRevenue(from, to)).thenReturn(List.of(
                new FakeAggregateRow("USD", "PROMOTION", new BigDecimal("150.00"), 42L),
                new FakeAggregateRow("USD", "ADVERTISEMENT", new BigDecimal("200.00"), 18L),
                new FakeAggregateRow("KHR", "PROMOTION", new BigDecimal("600000"), 30L),
                new FakeAggregateRow("KHR", "ADVERTISEMENT", new BigDecimal("800000"), 20L)));

        PlatformRevenueSummaryResponse response = service.getPlatformRevenueSummary(from, to);

        PlatformRevenueCurrencySummary usd = findCurrency(response.currencies(), "USD");
        assertThat(usd.promotionRevenue()).isEqualByComparingTo("150.00");
        assertThat(usd.advertisementRevenue()).isEqualByComparingTo("200.00");
        assertThat(usd.totalRevenue()).isEqualByComparingTo("350.00");
        assertThat(usd.promotionTransactions()).isEqualTo(42);
        assertThat(usd.advertisementTransactions()).isEqualTo(18);
        assertThat(usd.totalTransactions()).isEqualTo(60);

        PlatformRevenueCurrencySummary khr = findCurrency(response.currencies(), "KHR");
        assertThat(khr.totalRevenue()).isEqualByComparingTo("1400000");
        assertThat(khr.totalTransactions()).isEqualTo(50);
    }

    @Test
    void getPlatformRevenueSummary_multiCurrency_neverCombinesUsdAndKhrIntoOneTotal() {
        OffsetDateTime from = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-09-01T00:00:00Z");
        when(walletTransactionRepository.aggregatePlatformRevenue(from, to)).thenReturn(List.of(
                new FakeAggregateRow("USD", "PROMOTION", new BigDecimal("5.00"), 1L),
                new FakeAggregateRow("USD", "ADVERTISEMENT", new BigDecimal("6.00"), 1L),
                new FakeAggregateRow("KHR", "PROMOTION", new BigDecimal("20000"), 1L),
                new FakeAggregateRow("KHR", "ADVERTISEMENT", new BigDecimal("24000"), 1L)));

        PlatformRevenueSummaryResponse response = service.getPlatformRevenueSummary(from, to);

        assertThat(response.currencies()).hasSize(2);
        assertThat(findCurrency(response.currencies(), "USD").totalRevenue()).isEqualByComparingTo("11.00");
        assertThat(findCurrency(response.currencies(), "KHR").totalRevenue()).isEqualByComparingTo("44000");
        // No 44011, no converted composite total — each currency stands alone.
    }

    @Test
    void getPlatformRevenueSummary_topUpAccounting_onlySpentAmountsCountAsRevenue() {
        // The DB query itself only ever returns PROMOTION/ADVERTISEMENT OUT rows (TOP_UP is
        // structurally excluded by the SQL's `transaction_type in (...)` filter) — this proves
        // that once that filtering has happened, a $20 top-up followed by $5 + $6 of spend
        // correctly reports $11 of revenue, not $31 and not the $20 top-up amount.
        OffsetDateTime from = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-09-01T00:00:00Z");
        when(walletTransactionRepository.aggregatePlatformRevenue(from, to)).thenReturn(List.of(
                new FakeAggregateRow("USD", "PROMOTION", new BigDecimal("5.00"), 1L),
                new FakeAggregateRow("USD", "ADVERTISEMENT", new BigDecimal("6.00"), 1L)));

        PlatformRevenueSummaryResponse response = service.getPlatformRevenueSummary(from, to);

        assertThat(findCurrency(response.currencies(), "USD").totalRevenue()).isEqualByComparingTo("11.00");
    }

    @Test
    void getPlatformRevenueSummary_ignoresUnexpectedTransactionTypeOrCurrency_defensively() {
        // Defense in depth: even if a row somehow arrived with a type/currency outside the
        // supported set, the service must not fold it into revenue.
        OffsetDateTime from = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-09-01T00:00:00Z");
        when(walletTransactionRepository.aggregatePlatformRevenue(from, to)).thenReturn(List.of(
                new FakeAggregateRow("USD", "PROMOTION", new BigDecimal("5.00"), 1L),
                new FakeAggregateRow("USD", "TOP_UP", new BigDecimal("999.00"), 1L),
                new FakeAggregateRow("EUR", "PROMOTION", new BigDecimal("999.00"), 1L)));

        PlatformRevenueSummaryResponse response = service.getPlatformRevenueSummary(from, to);

        assertThat(response.currencies()).hasSize(2);
        assertThat(findCurrency(response.currencies(), "USD").totalRevenue()).isEqualByComparingTo("5.00");
    }

    @Test
    void getPlatformRevenueSummary_emptyResult_returnsZeroForBothCurrencies_notNull() {
        OffsetDateTime from = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-09-01T00:00:00Z");
        when(walletTransactionRepository.aggregatePlatformRevenue(from, to)).thenReturn(List.of());

        PlatformRevenueSummaryResponse response = service.getPlatformRevenueSummary(from, to);

        assertThat(response.currencies()).hasSize(2);
        for (PlatformRevenueCurrencySummary summary : response.currencies()) {
            assertThat(summary.totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(summary.promotionRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(summary.advertisementRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(summary.totalTransactions()).isZero();
        }
    }

    @Test
    void getPlatformRevenueSummary_nullFrom_rejected() {
        assertThatThrownBy(() -> service.getPlatformRevenueSummary(null, OffsetDateTime.now()))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void getPlatformRevenueSummary_nullTo_rejected() {
        assertThatThrownBy(() -> service.getPlatformRevenueSummary(OffsetDateTime.now(), null))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void getPlatformRevenueSummary_fromEqualsTo_rejected() {
        OffsetDateTime instant = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        assertThatThrownBy(() -> service.getPlatformRevenueSummary(instant, instant))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void getPlatformRevenueSummary_fromAfterTo_rejected() {
        OffsetDateTime from = OffsetDateTime.parse("2026-09-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        assertThatThrownBy(() -> service.getPlatformRevenueSummary(from, to))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void getPlatformRevenueSummary_rangeExceedsOneYear_rejected() {
        OffsetDateTime from = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        OffsetDateTime to = from.plusYears(1).plusDays(1);
        assertThatThrownBy(() -> service.getPlatformRevenueSummary(from, to))
                .isInstanceOf(InvalidOperationException.class);
    }

    private PlatformRevenueCurrencySummary findCurrency(List<PlatformRevenueCurrencySummary> summaries, String currency) {
        return summaries.stream().filter(s -> s.currency().equals(currency)).findFirst()
                .orElseThrow(() -> new AssertionError("Missing currency: " + currency));
    }

    // ---------------------------------------------------------------
    // Breakdown — percentages
    // ---------------------------------------------------------------

    @Test
    void getPlatformRevenueBreakdown_calculatesPercentages_summingToApproximately100() {
        OffsetDateTime from = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-09-01T00:00:00Z");
        when(walletTransactionRepository.aggregatePlatformRevenue(from, to)).thenReturn(List.of(
                new FakeAggregateRow("USD", "PROMOTION", new BigDecimal("150.00"), 42L),
                new FakeAggregateRow("USD", "ADVERTISEMENT", new BigDecimal("200.00"), 18L)));

        PlatformRevenueBreakdownResponse response = service.getPlatformRevenueBreakdown(from, to);

        PlatformRevenueCurrencyBreakdown usd = response.currencies().stream()
                .filter(c -> c.currency().equals("USD")).findFirst().orElseThrow();
        PlatformRevenueSourceBreakdown promotion = source(usd, "PROMOTION");
        PlatformRevenueSourceBreakdown advertisement = source(usd, "ADVERTISEMENT");

        assertThat(promotion.percentage()).isEqualByComparingTo("42.86");
        assertThat(advertisement.percentage()).isEqualByComparingTo("57.14");
        assertThat(promotion.percentage().add(advertisement.percentage()))
                .isCloseTo(new BigDecimal("100"), org.assertj.core.data.Offset.offset(new BigDecimal("0.05")));
        assertThat(promotion.transactionCount()).isEqualTo(42);
        assertThat(advertisement.transactionCount()).isEqualTo(18);
    }

    @Test
    void getPlatformRevenueBreakdown_zeroTotal_percentageZero() {
        OffsetDateTime from = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-09-01T00:00:00Z");
        when(walletTransactionRepository.aggregatePlatformRevenue(from, to)).thenReturn(List.of());

        PlatformRevenueBreakdownResponse response = service.getPlatformRevenueBreakdown(from, to);

        for (PlatformRevenueCurrencyBreakdown currency : response.currencies()) {
            for (PlatformRevenueSourceBreakdown source : currency.sources()) {
                assertThat(source.percentage()).isEqualByComparingTo(BigDecimal.ZERO);
            }
        }
    }

    @Test
    void getPlatformRevenueBreakdown_currenciesReportedSeparately() {
        OffsetDateTime from = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-09-01T00:00:00Z");
        when(walletTransactionRepository.aggregatePlatformRevenue(from, to)).thenReturn(List.of(
                new FakeAggregateRow("USD", "PROMOTION", new BigDecimal("10.00"), 1L),
                new FakeAggregateRow("KHR", "ADVERTISEMENT", new BigDecimal("40000"), 1L)));

        PlatformRevenueBreakdownResponse response = service.getPlatformRevenueBreakdown(from, to);

        assertThat(response.currencies()).hasSize(2);
        PlatformRevenueCurrencyBreakdown khr = response.currencies().stream()
                .filter(c -> c.currency().equals("KHR")).findFirst().orElseThrow();
        assertThat(source(khr, "ADVERTISEMENT").percentage()).isEqualByComparingTo("100.00");
        assertThat(source(khr, "PROMOTION").percentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private PlatformRevenueSourceBreakdown source(PlatformRevenueCurrencyBreakdown breakdown, String source) {
        return breakdown.sources().stream().filter(s -> s.source().equals(source)).findFirst().orElseThrow();
    }

    // ---------------------------------------------------------------
    // Trend — DAY/MONTH grouping, zero-fill
    // ---------------------------------------------------------------

    @Test
    void getPlatformRevenueTrend_day_zeroFillsMissingPeriods() {
        OffsetDateTime from = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-08-04T00:00:00Z");
        when(walletTransactionRepository.aggregatePlatformRevenueByDay(from, to)).thenReturn(List.of(
                new FakeTrendRow(Date.valueOf(LocalDate.of(2026, 8, 1)), "USD", "PROMOTION", new BigDecimal("8.00"), 2L),
                new FakeTrendRow(Date.valueOf(LocalDate.of(2026, 8, 1)), "USD", "ADVERTISEMENT", new BigDecimal("12.00"), 3L),
                new FakeTrendRow(Date.valueOf(LocalDate.of(2026, 8, 3)), "USD", "PROMOTION", new BigDecimal("5.00"), 1L)));

        PlatformRevenueTrendResponse response = service.getPlatformRevenueTrend(from, to, GroupBy.DAY);

        PlatformRevenueCurrencyTrend usd = response.currencies().stream()
                .filter(c -> c.currency().equals("USD")).findFirst().orElseThrow();
        assertThat(usd.points()).hasSize(3);
        assertThat(usd.points().get(0).period()).isEqualTo("2026-08-01");
        assertThat(usd.points().get(0).totalRevenue()).isEqualByComparingTo("20.00");
        assertThat(usd.points().get(1).period()).isEqualTo("2026-08-02");
        assertThat(usd.points().get(1).totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(usd.points().get(1).transactionCount()).isZero();
        assertThat(usd.points().get(2).period()).isEqualTo("2026-08-03");
        assertThat(usd.points().get(2).totalRevenue()).isEqualByComparingTo("5.00");
    }

    @Test
    void getPlatformRevenueTrend_month_formatsPeriodAsYearMonth() {
        OffsetDateTime from = OffsetDateTime.parse("2026-06-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-09-01T00:00:00Z");
        when(walletTransactionRepository.aggregatePlatformRevenueByMonth(from, to)).thenReturn(List.of(
                new FakeTrendRow(Date.valueOf(LocalDate.of(2026, 8, 1)), "USD", "PROMOTION", new BigDecimal("5.00"), 1L)));

        PlatformRevenueTrendResponse response = service.getPlatformRevenueTrend(from, to, GroupBy.MONTH);

        PlatformRevenueCurrencyTrend usd = response.currencies().stream()
                .filter(c -> c.currency().equals("USD")).findFirst().orElseThrow();
        assertThat(usd.points()).extracting(PlatformRevenueTrendPoint::period)
                .containsExactly("2026-06", "2026-07", "2026-08");
        assertThat(usd.points().get(2).totalRevenue()).isEqualByComparingTo("5.00");
    }

    @Test
    void getPlatformRevenueTrend_currencyAndSourceSplit() {
        OffsetDateTime from = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-08-02T00:00:00Z");
        when(walletTransactionRepository.aggregatePlatformRevenueByDay(from, to)).thenReturn(List.of(
                new FakeTrendRow(Date.valueOf(LocalDate.of(2026, 8, 1)), "USD", "PROMOTION", new BigDecimal("5.00"), 1L),
                new FakeTrendRow(Date.valueOf(LocalDate.of(2026, 8, 1)), "KHR", "ADVERTISEMENT", new BigDecimal("24000"), 1L)));

        PlatformRevenueTrendResponse response = service.getPlatformRevenueTrend(from, to, GroupBy.DAY);

        assertThat(response.currencies()).hasSize(2);
        PlatformRevenueCurrencyTrend usd = response.currencies().stream()
                .filter(c -> c.currency().equals("USD")).findFirst().orElseThrow();
        PlatformRevenueCurrencyTrend khr = response.currencies().stream()
                .filter(c -> c.currency().equals("KHR")).findFirst().orElseThrow();
        assertThat(usd.points().get(0).promotionRevenue()).isEqualByComparingTo("5.00");
        assertThat(usd.points().get(0).advertisementRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(khr.points().get(0).advertisementRevenue()).isEqualByComparingTo("24000");
        assertThat(khr.points().get(0).promotionRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getPlatformRevenueTrend_nullFrom_rejected() {
        assertThatThrownBy(() -> service.getPlatformRevenueTrend(null, OffsetDateTime.now(), GroupBy.DAY))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void getPlatformRevenueTrend_fromAfterTo_rejected() {
        OffsetDateTime from = OffsetDateTime.parse("2026-09-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        assertThatThrownBy(() -> service.getPlatformRevenueTrend(from, to, GroupBy.DAY))
                .isInstanceOf(InvalidOperationException.class);
    }

    // ---------------------------------------------------------------
    // Vendor Booking Value report — replaces the always-zero WalletTransaction-based Vendor
    // Earnings report. Source of truth is BookingRepository, COMPLETED status, subtotal field —
    // the exact same definition VendorPerformanceServiceImpl.completedBookingValue uses. Never
    // WalletTransaction; never combines USD/KHR.
    // ---------------------------------------------------------------

    private static final String OWNER_ID = "vendor-1";

    @Test
    void getVendorBookingValueReport_sumsCompletedBookings_perCurrency() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        when(bookingRepository.sumCompletedBookingValueByOwnerAndCurrency(
                eq(OWNER_ID), eq(BookingStatus.COMPLETED), any(), any()))
                .thenReturn(List.of(new FakeVendorCurrencyRow("USD", new BigDecimal("1250.00"), 18L)));
        when(bookingRepository.aggregateCompletedBookingValueByOwnerAndDay(eq(OWNER_ID), any(), any()))
                .thenReturn(List.of());

        VendorBookingValueReportResponse response = service.getVendorBookingValueReport(OWNER_ID, from, to, GroupBy.DAY);

        VendorBookingValueCurrencySummary usd = findVendorCurrency(response.currencies(), "USD");
        assertThat(usd.completedBookingValue()).isEqualByComparingTo("1250.00");
        assertThat(usd.completedBookingCount()).isEqualTo(18L);
        // 1250.00 / 18 = 69.44 (HALF_UP, scale 2)
        assertThat(usd.averageBookingValue()).isEqualByComparingTo("69.44");
    }

    @Test
    void getVendorBookingValueReport_noCompletedBookings_returnsZero() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        when(bookingRepository.sumCompletedBookingValueByOwnerAndCurrency(any(), any(), any(), any())).thenReturn(List.of());
        when(bookingRepository.aggregateCompletedBookingValueByOwnerAndDay(any(), any(), any())).thenReturn(List.of());

        VendorBookingValueReportResponse response = service.getVendorBookingValueReport(OWNER_ID, from, to, GroupBy.DAY);

        VendorBookingValueCurrencySummary usd = findVendorCurrency(response.currencies(), "USD");
        assertThat(usd.completedBookingValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(usd.completedBookingCount()).isZero();
        assertThat(usd.averageBookingValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getVendorBookingValueReport_neverCombinesUsdAndKhr() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        when(bookingRepository.sumCompletedBookingValueByOwnerAndCurrency(any(), any(), any(), any())).thenReturn(List.of(
                new FakeVendorCurrencyRow("USD", new BigDecimal("150.00"), 2L),
                new FakeVendorCurrencyRow("KHR", new BigDecimal("400000"), 1L)));
        when(bookingRepository.aggregateCompletedBookingValueByOwnerAndDay(any(), any(), any())).thenReturn(List.of());

        VendorBookingValueReportResponse response = service.getVendorBookingValueReport(OWNER_ID, from, to, GroupBy.DAY);

        assertThat(findVendorCurrency(response.currencies(), "USD").completedBookingValue()).isEqualByComparingTo("150.00");
        assertThat(findVendorCurrency(response.currencies(), "KHR").completedBookingValue()).isEqualByComparingTo("400000");
        // Never merged into one number (e.g. never 400150).
        assertThat(response.currencies()).hasSize(2);
    }

    @Test
    void getVendorBookingValueReport_scopesToAuthenticatedOwnerId_andCompletedStatusOnly() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        when(bookingRepository.sumCompletedBookingValueByOwnerAndCurrency(any(), any(), any(), any())).thenReturn(List.of());
        when(bookingRepository.aggregateCompletedBookingValueByOwnerAndDay(any(), any(), any())).thenReturn(List.of());

        service.getVendorBookingValueReport(OWNER_ID, from, to, GroupBy.DAY);

        // Ownership (owner-scoped WHERE clause) and status filtering (COMPLETED only — PENDING/
        // CANCELLED/other-vendor bookings excluded) are enforced inside the repository's own
        // query definition; this verifies the service passes exactly those arguments through,
        // the same verification style already used for VendorPerformanceServiceImplTest.
        verify(bookingRepository).sumCompletedBookingValueByOwnerAndCurrency(
                eq(OWNER_ID), eq(BookingStatus.COMPLETED), any(), any());
    }

    @Test
    void getVendorBookingValueReport_neverQueriesWalletTransactionRepository() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        when(bookingRepository.sumCompletedBookingValueByOwnerAndCurrency(any(), any(), any(), any())).thenReturn(List.of());
        when(bookingRepository.aggregateCompletedBookingValueByOwnerAndDay(any(), any(), any())).thenReturn(List.of());

        service.getVendorBookingValueReport(OWNER_ID, from, to, GroupBy.DAY);

        // No booking-linked wallet earnings query exists any more to call — proven both
        // structurally (sumVendorEarningsTotalsForOwner/aggregateVendorEarningsBy* no longer
        // exist on WalletTransactionRepository) and behaviorally here.
        verifyNoInteractions(walletTransactionRepository);
    }

    @Test
    void getVendorBookingValueReport_rejectsFromAfterTo() {
        assertThatThrownBy(() -> service.getVendorBookingValueReport(
                OWNER_ID, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 1, 1), GroupBy.DAY))
                .isInstanceOf(InvalidOperationException.class);
        verify(bookingRepository, never()).sumCompletedBookingValueByOwnerAndCurrency(any(), any(), any(), any());
    }

    @Test
    void getVendorBookingValueReport_rejectsRangeExceedingOneYear() {
        assertThatThrownBy(() -> service.getVendorBookingValueReport(
                OWNER_ID, LocalDate.of(2024, 1, 1), LocalDate.of(2026, 1, 2), GroupBy.DAY))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void getVendorBookingValueReport_dayTrend_zeroFillsMissingDaysAndSeparatesCurrency() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 3);

        when(bookingRepository.sumCompletedBookingValueByOwnerAndCurrency(any(), any(), any(), any())).thenReturn(List.of());
        when(bookingRepository.aggregateCompletedBookingValueByOwnerAndDay(eq(OWNER_ID), any(), any())).thenReturn(List.of(
                new FakeVendorTrendRow(Date.valueOf(LocalDate.of(2026, 8, 2)), "USD", new BigDecimal("100.00"), 3L)));

        VendorBookingValueReportResponse response = service.getVendorBookingValueReport(OWNER_ID, from, to, GroupBy.DAY);

        VendorBookingValueCurrencyTrend usd = response.trend().stream()
                .filter(t -> t.currency().equals("USD")).findFirst().orElseThrow();
        assertThat(usd.points()).hasSize(3);
        assertThat(usd.points().get(0).period()).isEqualTo("2026-08-01");
        assertThat(usd.points().get(0).completedBookingValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(usd.points().get(1).period()).isEqualTo("2026-08-02");
        assertThat(usd.points().get(1).completedBookingValue()).isEqualByComparingTo("100.00");
        assertThat(usd.points().get(1).completedBookingCount()).isEqualTo(3L);
        assertThat(usd.points().get(2).completedBookingValue()).isEqualByComparingTo(BigDecimal.ZERO);

        VendorBookingValueCurrencyTrend khr = response.trend().stream()
                .filter(t -> t.currency().equals("KHR")).findFirst().orElseThrow();
        assertThat(khr.points()).allSatisfy(point -> assertThat(point.completedBookingValue()).isEqualByComparingTo(BigDecimal.ZERO));
    }

    @Test
    void getVendorBookingValueReport_monthGrouping_usesMonthAggregation() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 8, 1);

        when(bookingRepository.sumCompletedBookingValueByOwnerAndCurrency(any(), any(), any(), any())).thenReturn(List.of());
        when(bookingRepository.aggregateCompletedBookingValueByOwnerAndMonth(eq(OWNER_ID), any(), any())).thenReturn(List.of(
                new FakeVendorTrendRow(Date.valueOf(LocalDate.of(2026, 7, 1)), "USD", new BigDecimal("500.00"), 7L)));

        VendorBookingValueReportResponse response = service.getVendorBookingValueReport(OWNER_ID, from, to, GroupBy.MONTH);

        VendorBookingValueCurrencyTrend usd = response.trend().stream()
                .filter(t -> t.currency().equals("USD")).findFirst().orElseThrow();
        assertThat(usd.points()).extracting(point -> point.period())
                .containsExactly("2026-06", "2026-07", "2026-08");
        assertThat(usd.points().get(1).completedBookingValue()).isEqualByComparingTo("500.00");
        verify(bookingRepository, never()).aggregateCompletedBookingValueByOwnerAndDay(any(), any(), any());
    }

    private VendorBookingValueCurrencySummary findVendorCurrency(
            List<VendorBookingValueCurrencySummary> currencies, String currency) {
        return currencies.stream().filter(c -> c.currency().equals(currency)).findFirst().orElseThrow();
    }

    // ---------------------------------------------------------------
    // Consistency with VendorPerformanceServiceImpl.completedBookingValue — both must reconcile
    // for equivalent filters (same Vendor, same COMPLETED status, same Booking.subtotal field),
    // proving there is only one business definition of "Vendor completed booking value", not
    // two independently-implemented ones.
    // ---------------------------------------------------------------

    @Test
    void vendorBookingValueReport_reconcilesWith_vendorPerformanceCompletedBookingValue() {
        when(bookingRepository.sumCompletedBookingValueByOwnerAndCurrency(
                eq(OWNER_ID), eq(BookingStatus.COMPLETED), any(), any()))
                .thenReturn(List.of(new FakeVendorCurrencyRow("USD", new BigDecimal("100.00"), 1L)));
        when(bookingRepository.aggregateCompletedBookingValueByOwnerAndDay(eq(OWNER_ID), any(), any()))
                .thenReturn(List.of());
        // VendorPerformance is lifetime (no date range) — its own repository call takes only
        // ownerId/status, independent of the report's date range.
        when(bookingRepository.sumSubtotalByOwnerIdAndStatus(OWNER_ID, BookingStatus.COMPLETED))
                .thenReturn(new BigDecimal("100.00"));

        VendorBookingValueReportResponse report = service.getVendorBookingValueReport(
                OWNER_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), GroupBy.DAY);
        VendorBookingValueCurrencySummary usd = findVendorCurrency(report.currencies(), "USD");

        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findById(OWNER_ID)).thenReturn(
                Optional.of(User.builder().id(OWNER_ID).accountStatus(AccountStatus.ACTIVE).build()));
        VendorPerformanceServiceImpl performanceService = new VendorPerformanceServiceImpl(
                userRepository, bookingRepository, mock(BookingStatusHistoryRepository.class),
                mock(ReviewRepository.class), mock(VendorStatusAuditRepository.class), mock(Keycloak.class),
                mock(KeycloakAdminClientProps.class), mock(AdminAuditService.class));
        VendorPerformanceResponse performance = performanceService.getPerformance(OWNER_ID);

        assertThat(performance.completedBookingValue()).isEqualByComparingTo(usd.completedBookingValue());
    }
}
