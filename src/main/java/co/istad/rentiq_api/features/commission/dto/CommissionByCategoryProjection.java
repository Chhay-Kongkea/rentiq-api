package co.istad.rentiq_api.features.commission.dto;

import java.math.BigDecimal;

public interface CommissionByCategoryProjection {
    Short getCategoryId();

    BigDecimal getTotalCommission();

    Long getBookingCount();
}
