package co.istad.rentiq_api.features.financialReport.dto.projection;

import java.math.BigDecimal;
import java.sql.Date;

public interface VendorEarningsPeriodProjection {

    Date getPeriod();

    BigDecimal getTotalEarnings();

    Long getTransactionCount();
}
