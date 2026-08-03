package co.istad.rentiq_api.features.wallet.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateTopupRequest(

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount,

        String paymentMethod

) {}
