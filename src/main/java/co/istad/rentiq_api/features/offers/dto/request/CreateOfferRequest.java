package co.istad.rentiq_api.features.offers.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOfferRequest(

        @NotNull
        @DecimalMin("0.01")
        BigDecimal offeredPrice,

        String currency,

        String message,

        @NotNull
        java.util.UUID itemId

) {}