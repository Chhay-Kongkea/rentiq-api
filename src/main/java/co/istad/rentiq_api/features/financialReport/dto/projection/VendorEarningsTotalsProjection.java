package co.istad.rentiq_api.features.financialReport.dto.projection;

import java.math.BigDecimal;

public interface VendorEarningsTotalsProjection {

    BigDecimal getTotalEarnings();

    Long getTransactionCount();
}
