package co.istad.rentiq_api.features.itemrequest.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOfferRequest(

        UUID itemId,

        @NotNull(message = "Offered price is required")
        @DecimalMin(value = "0.01", message = "Offered price must be greater than zero")
        BigDecimal offeredPrice,

        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must contain exactly three letters")
        String currency,

        @Size(max = 2000)
        String message

) {
    public CreateOfferRequest {
        currency = currency == null || currency.isBlank()
                ? "USD"
                : currency.trim().toUpperCase();

        message = message == null || message.isBlank()
                ? null
                : message.trim();
    }
}
