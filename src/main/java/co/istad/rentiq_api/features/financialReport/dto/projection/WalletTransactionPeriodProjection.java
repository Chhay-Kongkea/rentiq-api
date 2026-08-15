package co.istad.rentiq_api.features.financialReport.dto.projection;

import java.math.BigDecimal;
import java.sql.Date;

public interface WalletTransactionPeriodProjection {

    Date getPeriod();

    String getTransactionType();

    String getDirection();

    BigDecimal getTotalAmount();

    Long getTransactionCount();
}
