package co.istad.rentiq_api.features.itemrequest.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateOfferRequest(

        UUID itemId,

        @DecimalMin("0.01")
        BigDecimal offeredPrice,

        @Pattern(
                regexp = "^[A-Za-z]{3}$",
                message = "Currency must contain exactly three letters"
        )
        String currency,

        @Size(max = 2000)
        String message

) {
    public UpdateOfferRequest {
        currency = currency == null || currency.isBlank()
                ? null
                : currency.trim().toUpperCase();

        message = message == null || message.isBlank()
                ? null
                : message.trim();
    }
}