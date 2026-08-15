package co.istad.rentiq_api.features.commission.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * commissionRate is a fraction, not a percentage: 0.1000 == 10%, matching
 * categories.commission_rate (NUMERIC(5,4)) and the existing category DTOs.
 */
public record UpdateCommissionRateRequest(

        @NotNull
        @DecimalMin(value = "0.0000")
        @DecimalMax(value = "1.0000")
        BigDecimal commissionRate,

        @NotBlank
        String reason

) {}
