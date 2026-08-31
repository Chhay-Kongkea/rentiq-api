package co.istad.rentiq_api.features.financialReport.dto.projection;

import java.math.BigDecimal;
import java.sql.Date;

/**
 * Per-period aggregate: one row per (period, currency, transactionType). The service zero-fills
 * missing period buckets in Java after receiving this small, already-aggregated result set —
 * see FinancialReportServiceImpl.getPlatformRevenueTrend.
 */
public interface PlatformRevenueTrendAggregateProjection {

    Date getPeriod();

    String getCurrency();

    String getTransactionType();

    BigDecimal getTotalAmount();

    Long getTransactionCount();
}
