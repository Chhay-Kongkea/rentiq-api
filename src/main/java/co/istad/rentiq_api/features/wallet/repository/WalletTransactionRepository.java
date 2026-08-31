package co.istad.rentiq_api.features.wallet.repository;


import co.istad.rentiq_api.features.financialReport.dto.projection.PlatformRevenueAggregateProjection;
import co.istad.rentiq_api.features.financialReport.dto.projection.PlatformRevenueTrendAggregateProjection;
import co.istad.rentiq_api.features.financialReport.dto.projection.WalletTransactionPeriodProjection;
import co.istad.rentiq_api.features.financialReport.dto.projection.WalletTransactionTypeSummaryProjection;
import co.istad.rentiq_api.features.wallet.entity.WalletTransaction;
import co.istad.rentiq_api.features.wallet.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    boolean existsByWalletIdAndTransactionType(UUID walletId, TransactionType transactionType);

    Page<WalletTransaction> findByWalletId(UUID walletId, Pageable pageable);

    // Admin transactions report — no date_trunc, whole-range summary by type/direction.
    @Query(value = """
            select wt.transaction_type as transactionType,
                   wt.direction as direction,
                   coalesce(sum(wt.amount), 0) as totalAmount,
                   count(*) as transactionCount
            from wallet_transactions wt
            where wt.created_at >= :from and wt.created_at < :to
            group by wt.transaction_type, wt.direction
            order by wt.transaction_type, wt.direction
            """, nativeQuery = true)
    List<WalletTransactionTypeSummaryProjection> sumTransactionTotalsByTypeAndDirection(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    // Per-period breakdown for the same report, grouped by period x type x direction.
    // date_trunc normalized to UTC — see BookingRepository's aggregate queries for why.
    @Query(
            value = """
                    select cast(date_trunc('day', wt.created_at at time zone 'UTC') as date) as period,
                           wt.transaction_type as transactionType,
                           wt.direction as direction,
                           coalesce(sum(wt.amount), 0) as totalAmount,
                           count(*) as transactionCount
                    from wallet_transactions wt
                    where wt.created_at >= :from and wt.created_at < :to
                    group by date_trunc('day', wt.created_at at time zone 'UTC'), wt.transaction_type, wt.direction
                    order by period, wt.transaction_type, wt.direction
                    """,
            countQuery = """
                    select count(*) from (
                      select 1 from wallet_transactions wt
                      where wt.created_at >= :from and wt.created_at < :to
                      group by date_trunc('day', wt.created_at at time zone 'UTC'), wt.transaction_type, wt.direction
                    ) sub
                    """,
            nativeQuery = true)
    Page<WalletTransactionPeriodProjection> aggregateTransactionsByDay(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable);

    @Query(
            value = """
                    select cast(date_trunc('month', wt.created_at at time zone 'UTC') as date) as period,
                           wt.transaction_type as transactionType,
                           wt.direction as direction,
                           coalesce(sum(wt.amount), 0) as totalAmount,
                           count(*) as transactionCount
                    from wallet_transactions wt
                    where wt.created_at >= :from and wt.created_at < :to
                    group by date_trunc('month', wt.created_at at time zone 'UTC'), wt.transaction_type, wt.direction
                    order by period, wt.transaction_type, wt.direction
                    """,
            countQuery = """
                    select count(*) from (
                      select 1 from wallet_transactions wt
                      where wt.created_at >= :from and wt.created_at < :to
                      group by date_trunc('month', wt.created_at at time zone 'UTC'), wt.transaction_type, wt.direction
                    ) sub
                    """,
            nativeQuery = true)
    Page<WalletTransactionPeriodProjection> aggregateTransactionsByMonth(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable);

    // ---------------------------------------------------------------
    // Platform Revenue — the source of truth is PROMOTION/ADVERTISEMENT OUT ledger entries
    // only (never TOP_UP, ADMIN_ADJUSTMENT, or booking data). Currency is not a column on
    // wallet_transactions itself — it belongs to the owning wallet — hence the join, same as
    // the vendor-earnings queries above.
    // ---------------------------------------------------------------

    @Query(value = """
            select ow.currency as currency,
                   wt.transaction_type as transactionType,
                   coalesce(sum(wt.amount), 0) as totalAmount,
                   count(*) as transactionCount
            from wallet_transactions wt
            join owner_wallets ow on ow.id = wt.wallet_id
            where wt.direction = 'OUT'
              and wt.transaction_type in ('PROMOTION', 'ADVERTISEMENT')
              and wt.created_at >= :from and wt.created_at < :to
            group by ow.currency, wt.transaction_type
            order by ow.currency, wt.transaction_type
            """, nativeQuery = true)
    List<PlatformRevenueAggregateProjection> aggregatePlatformRevenue(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    @Query(value = """
            select cast(date_trunc('day', wt.created_at at time zone 'UTC') as date) as period,
                   ow.currency as currency,
                   wt.transaction_type as transactionType,
                   coalesce(sum(wt.amount), 0) as totalAmount,
                   count(*) as transactionCount
            from wallet_transactions wt
            join owner_wallets ow on ow.id = wt.wallet_id
            where wt.direction = 'OUT'
              and wt.transaction_type in ('PROMOTION', 'ADVERTISEMENT')
              and wt.created_at >= :from and wt.created_at < :to
            group by date_trunc('day', wt.created_at at time zone 'UTC'), ow.currency, wt.transaction_type
            order by period, ow.currency, wt.transaction_type
            """, nativeQuery = true)
    List<PlatformRevenueTrendAggregateProjection> aggregatePlatformRevenueByDay(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    @Query(value = """
            select cast(date_trunc('month', wt.created_at at time zone 'UTC') as date) as period,
                   ow.currency as currency,
                   wt.transaction_type as transactionType,
                   coalesce(sum(wt.amount), 0) as totalAmount,
                   count(*) as transactionCount
            from wallet_transactions wt
            join owner_wallets ow on ow.id = wt.wallet_id
            where wt.direction = 'OUT'
              and wt.transaction_type in ('PROMOTION', 'ADVERTISEMENT')
              and wt.created_at >= :from and wt.created_at < :to
            group by date_trunc('month', wt.created_at at time zone 'UTC'), ow.currency, wt.transaction_type
            order by period, ow.currency, wt.transaction_type
            """, nativeQuery = true)
    List<PlatformRevenueTrendAggregateProjection> aggregatePlatformRevenueByMonth(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);
}
