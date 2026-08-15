package co.istad.rentiq_api.features.wallet.dto.request;

import co.istad.rentiq_api.features.wallet.enums.TransactionDirection;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdminWalletAdjustRequest(

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount,

        @NotNull
        TransactionDirection direction,

        @NotBlank
        String reason

) {}
