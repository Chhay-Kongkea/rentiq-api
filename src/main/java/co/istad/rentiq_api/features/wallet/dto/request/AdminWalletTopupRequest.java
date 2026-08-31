package co.istad.rentiq_api.features.wallet.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Vendor pays the admin externally (cash / ABA / KHQR / bank transfer) and the admin credits
 * the vendor's Rentiq platform-balance wallet after verifying that payment. No gateway, no
 * currency conversion — request.currency must match the wallet's own currency exactly.
 */
public record AdminWalletTopupRequest(

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount,

        @NotBlank
        @Size(max = 3)
        String currency,

        String paymentMethod,

        String paymentReference,

        String note

) {}
