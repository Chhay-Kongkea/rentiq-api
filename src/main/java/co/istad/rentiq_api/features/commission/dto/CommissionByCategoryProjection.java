package co.istad.rentiq_api.features.commission.dto;

import java.math.BigDecimal;
import java.util.UUID;

public interface CommissionByCategoryProjection {
    UUID getCategoryId();

    BigDecimal getTotalCommission();

    Long getBookingCount();
}
