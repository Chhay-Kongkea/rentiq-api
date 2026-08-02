package co.istad.rentiq_api.features.financialReport.dto.projection;

import java.math.BigDecimal;

public interface WalletTransactionTypeSummaryProjection {

    String getTransactionType();

    String getDirection();

    BigDecimal getTotalAmount();

    Long getTransactionCount();
}
