package co.istad.rentiq_api.features.bookings.repository;

import co.istad.rentiq_api.features.bookings.entity.Booking;
import co.istad.rentiq_api.features.bookings.enums.BookingStatus;
import co.istad.rentiq_api.features.bookings.enums.PaymentStatus;
import co.istad.rentiq_api.features.commission.dto.CommissionByCategoryProjection;
import co.istad.rentiq_api.features.financialReport.dto.projection.BookingPeriodAggregateProjection;
import co.istad.rentiq_api.features.financialReport.dto.projection.BookingTotalsProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface
BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<Booking> findByOwnerIdOrderByCreatedAtDesc(String ownerId);

    List<Booking> findByItem_IdAndStatusInOrderByRentalStartAsc(UUID itemId, List<BookingStatus> statuses);

    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

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


    @Query("""
            select i.categoryId as categoryId,
                   coalesce(sum(b.commissionAmount), 0.00) as totalCommission,
                   count(b) as bookingCount
            from Booking b
              join b.item i
            where b.paymentStatus = :paymentStatus
              and b.createdAt >= :from
              and b.createdAt < :to
            group by i.categoryId
            order by i.categoryId
            """)
    List<CommissionByCategoryProjection> aggregateCommissionByCategory(
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    long countByOwnerId(String ownerId);

    long countByOwnerIdAndStatus(String ownerId, BookingStatus status);


    @Query("""
            select coalesce(sum(b.subtotal), 0) as totalRevenue,
                   coalesce(sum(b.commissionAmount), 0) as totalCommission,
                   count(b) as bookingCount
            from Booking b
            where b.paymentStatus = :paymentStatus
              and b.createdAt >= :from
              and b.createdAt < :to
            """)
    BookingTotalsProjection sumRevenueAndCommissionTotals(
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);


    @Query(
            value = """
                    select cast(date_trunc('day', b.created_at at time zone 'UTC') as date) as period,
                           coalesce(sum(b.subtotal), 0) as totalRevenue,
                           coalesce(sum(b.commission_amount), 0) as totalCommission,
                           count(*) as bookingCount
                    from bookings b
                    where b.payment_status = :paymentStatus
                      and b.created_at >= :from
                      and b.created_at < :to
                    group by date_trunc('day', b.created_at at time zone 'UTC')
                    order by period
                    """,
            countQuery = """
                    select count(*) from (
                      select 1 from bookings b
                      where b.payment_status = :paymentStatus
                        and b.created_at >= :from and b.created_at < :to
                      group by date_trunc('day', b.created_at at time zone 'UTC')
                    ) sub
                    """,
            nativeQuery = true)
    Page<BookingPeriodAggregateProjection> aggregateRevenueAndCommissionByDay(
            @Param("paymentStatus") String paymentStatus,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable);

    @Query(
            value = """
                    select cast(date_trunc('month', b.created_at at time zone 'UTC') as date) as period,
                           coalesce(sum(b.subtotal), 0) as totalRevenue,
                           coalesce(sum(b.commission_amount), 0) as totalCommission,
                           count(*) as bookingCount
                    from bookings b
                    where b.payment_status = :paymentStatus
                      and b.created_at >= :from
                      and b.created_at < :to
                    group by date_trunc('month', b.created_at at time zone 'UTC')
                    order by period
                    """,
            countQuery = """
                    select count(*) from (
                      select 1 from bookings b
                      where b.payment_status = :paymentStatus
                        and b.created_at >= :from and b.created_at < :to
                      group by date_trunc('month', b.created_at at time zone 'UTC')
                    ) sub
                    """,
            nativeQuery = true)
    Page<BookingPeriodAggregateProjection> aggregateRevenueAndCommissionByMonth(
            @Param("paymentStatus") String paymentStatus,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable);
}

