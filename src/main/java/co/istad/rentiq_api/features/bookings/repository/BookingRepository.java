package co.istad.rentiq_api.features.bookings.repository;

import co.istad.rentiq_api.features.bookings.entity.Booking;
import co.istad.rentiq_api.features.bookings.enums.BookingStatus;
import co.istad.rentiq_api.features.commission.dto.CommissionByCategoryProjection;
import co.istad.rentiq_api.features.financialReport.dto.projection.BookingPeriodAggregateProjection;
import co.istad.rentiq_api.features.financialReport.dto.projection.BookingTotalsProjection;
import co.istad.rentiq_api.features.financialReport.dto.projection.VendorBookingValueCurrencyProjection;
import co.istad.rentiq_api.features.financialReport.dto.projection.VendorBookingValueTrendProjection;
import co.istad.rentiq_api.features.adminDashboard.projection.DashboardCountProjection;
import co.istad.rentiq_api.features.adminDashboard.projection.DashboardFinancialProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface
BookingRepository extends JpaRepository<Booking, UUID> {

    Page<Booking> findByCustomerId(String customerId, Pageable pageable);

    Page<Booking> findByOwnerId(String ownerId, Pageable pageable);

    List<Booking> findByItem_IdAndStatusInOrderByRentalStartAsc(UUID itemId, List<BookingStatus> statuses);

    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

    /**
     * Locks the booking row (SELECT ... FOR UPDATE) so a concurrent or repeated
     * release attempt for the same booking serializes instead of double-crediting.
     * Must only be called inside an existing @Transactional method.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") UUID id);

    long countByStatus(BookingStatus status);

    long countByStatusIn(List<BookingStatus> statuses);

    Page<Booking> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // All-time booking GMV/calculated-commission totals. Deliberately has no PaymentStatus
    // filter: Rentiq is P2P (rental payment never touches Rentiq), so no booking ever reaches
    // a "paid" payment status — filtering on one made this always return zero (backend audit
    // FIN-001). commissionAmount is CALCULATED only (never collected via any wallet
    // transaction) — never fold this into Platform Revenue.
    @Query("""
            select coalesce(sum(b.totalAmount), 0) as totalBookingValue,
                   coalesce(sum(b.commissionAmount), 0) as calculatedCommission
            from Booking b
            """)
    DashboardFinancialProjection sumDashboardFinancials();

    @Query(value = """
            select cast(date_trunc(:groupBy, b.created_at at time zone 'UTC') as date) as period,
                   count(*) as value
            from bookings b
            where b.created_at >= :from and b.created_at < :to
            group by date_trunc(:groupBy, b.created_at at time zone 'UTC')
            order by period
            """, nativeQuery = true)
    List<DashboardCountProjection> countBookingsByPeriod(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("groupBy") String groupBy);

    @Query("""
            select count(b) > 0 from Booking b
            where b.item.id = :itemId
              and b.status in :activeStatuses
              and b.rentalStart <= :end
              and b.rentalEnd >= :start
            """)
    boolean existsOverlappingBooking(@Param("itemId") UUID itemId,
                                      @Param("start") LocalDate start,
                                      @Param("end") LocalDate end,
                                      @Param("activeStatuses") List<BookingStatus> activeStatuses);

    @Query("""
            select b from Booking b
            where b.ownerId = :ownerId
              and (:from is null or b.rentalEnd >= :from)
              and (:to is null or b.rentalStart <= :to)
            order by b.rentalStart asc
            """)
    List<Booking> findScheduleByOwnerId(@Param("ownerId") String ownerId,
                                         @Param("from") LocalDate from,
                                         @Param("to") LocalDate to);


    // Calculated commission by category — no PaymentStatus filter (see sumDashboardFinancials).
    @Query("""
            select i.categoryId as categoryId,
                   coalesce(sum(b.commissionAmount), 0.00) as totalCommission,
                   count(b) as bookingCount
            from Booking b
              join b.item i
            where b.createdAt >= :from
              and b.createdAt < :to
            group by i.categoryId
            order by i.categoryId
            """)
    List<CommissionByCategoryProjection> aggregateCommissionByCategory(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    long countByOwnerId(String ownerId);

    long countByOwnerIdAndStatus(String ownerId, BookingStatus status);

    // Rental value arranged through Rentiq for this vendor's COMPLETED bookings — GMV, never
    // wallet earnings (rental payment is P2P, outside Rentiq; see VendorPerformanceServiceImpl).
    @Query("""
            select coalesce(sum(b.subtotal), 0) from Booking b
            where b.ownerId = :ownerId and b.status = :status
            """)
    BigDecimal sumSubtotalByOwnerIdAndStatus(@Param("ownerId") String ownerId, @Param("status") BookingStatus status);


    // Booking GMV + calculated commission for a date range — no PaymentStatus filter (see
    // sumDashboardFinancials). totalBookingValue is marketplace rental value arranged through
    // Rentiq, NOT Rentiq's own revenue (Rentiq never collects rental payment).
    @Query("""
            select coalesce(sum(b.subtotal), 0) as totalBookingValue,
                   coalesce(sum(b.commissionAmount), 0) as totalCommission,
                   count(b) as bookingCount
            from Booking b
            where b.createdAt >= :from
              and b.createdAt < :to
            """)
    BookingTotalsProjection sumBookingTotals(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);


    @Query(
            value = """
                    select cast(date_trunc('day', b.created_at at time zone 'UTC') as date) as period,
                           coalesce(sum(b.subtotal), 0) as totalBookingValue,
                           coalesce(sum(b.commission_amount), 0) as totalCommission,
                           count(*) as bookingCount
                    from bookings b
                    where b.created_at >= :from
                      and b.created_at < :to
                    group by date_trunc('day', b.created_at at time zone 'UTC')
                    order by period
                    """,
            countQuery = """
                    select count(*) from (
                      select 1 from bookings b
                      where b.created_at >= :from and b.created_at < :to
                      group by date_trunc('day', b.created_at at time zone 'UTC')
                    ) sub
                    """,
            nativeQuery = true)
    Page<BookingPeriodAggregateProjection> aggregateBookingTotalsByDay(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable);

    @Query(
            value = """
                    select cast(date_trunc('month', b.created_at at time zone 'UTC') as date) as period,
                           coalesce(sum(b.subtotal), 0) as totalBookingValue,
                           coalesce(sum(b.commission_amount), 0) as totalCommission,
                           count(*) as bookingCount
                    from bookings b
                    where b.created_at >= :from
                      and b.created_at < :to
                    group by date_trunc('month', b.created_at at time zone 'UTC')
                    order by period
                    """,
            countQuery = """
                    select count(*) from (
                      select 1 from bookings b
                      where b.created_at >= :from and b.created_at < :to
                      group by date_trunc('month', b.created_at at time zone 'UTC')
                    ) sub
                    """,
            nativeQuery = true)
    Page<BookingPeriodAggregateProjection> aggregateBookingTotalsByMonth(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable);


    // ---------------------------------------------------------------
    // Vendor Booking Value report — a single Vendor's own COMPLETED bookings, per currency, for
    // a date range. Source of truth is Booking.subtotal (the same field and COMPLETED status
    // VendorPerformanceServiceImpl.completedBookingValue already uses — one business definition,
    // never two), NOT WalletTransaction. Rental payment is P2P and never touches Rentiq, so this
    // is marketplace rental GMV, never Vendor wallet earnings.
    // ---------------------------------------------------------------

    @Query("""
            select b.currency as currency,
                   coalesce(sum(b.subtotal), 0) as totalBookingValue,
                   count(b) as bookingCount
            from Booking b
            where b.ownerId = :ownerId
              and b.status = :status
              and b.createdAt >= :from
              and b.createdAt < :to
            group by b.currency
            order by b.currency
            """)
    List<VendorBookingValueCurrencyProjection> sumCompletedBookingValueByOwnerAndCurrency(
            @Param("ownerId") String ownerId,
            @Param("status") BookingStatus status,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    // status is hardcoded to COMPLETED (native SQL, matching the aggregatePlatformRevenue*
    // convention of hardcoding the fixed set of qualifying types rather than parameterizing it).
    @Query(value = """
            select cast(date_trunc('day', b.created_at at time zone 'UTC') as date) as period,
                   b.currency as currency,
                   coalesce(sum(b.subtotal), 0) as totalBookingValue,
                   count(*) as bookingCount
            from bookings b
            where b.owner_id = :ownerId
              and b.status = 'COMPLETED'
              and b.created_at >= :from
              and b.created_at < :to
            group by date_trunc('day', b.created_at at time zone 'UTC'), b.currency
            order by period, b.currency
            """, nativeQuery = true)
    List<VendorBookingValueTrendProjection> aggregateCompletedBookingValueByOwnerAndDay(
            @Param("ownerId") String ownerId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    @Query(value = """
            select cast(date_trunc('month', b.created_at at time zone 'UTC') as date) as period,
                   b.currency as currency,
                   coalesce(sum(b.subtotal), 0) as totalBookingValue,
                   count(*) as bookingCount
            from bookings b
            where b.owner_id = :ownerId
              and b.status = 'COMPLETED'
              and b.created_at >= :from
              and b.created_at < :to
            group by date_trunc('month', b.created_at at time zone 'UTC'), b.currency
            order by period, b.currency
            """, nativeQuery = true)
    List<VendorBookingValueTrendProjection> aggregateCompletedBookingValueByOwnerAndMonth(
            @Param("ownerId") String ownerId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);
}
