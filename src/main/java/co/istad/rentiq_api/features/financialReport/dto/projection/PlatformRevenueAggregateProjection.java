package co.istad.rentiq_api.features.financialReport.dto.projection;

import java.math.BigDecimal;

/**
 * Whole-range aggregate: one row per (currency, transactionType) — always PROMOTION or
 * ADVERTISEMENT, always direction OUT (filtered in the query itself). Backs both the
 * platform-revenue summary and breakdown endpoints, since they need the same shape.
 */
public interface PlatformRevenueAggregateProjection {

    String getCurrency();

    String getTransactionType();

    BigDecimal getTotalAmount();

    Long getTransactionCount();
}
