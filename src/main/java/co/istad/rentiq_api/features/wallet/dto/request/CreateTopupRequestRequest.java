package co.istad.rentiq_api.features.wallet.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * A vendor asks the admin to credit their Rentiq platform-balance wallet. This only records a
 * PENDING request for an admin to review — it never mutates the wallet balance. The admin still
 * credits the wallet manually (after verifying the external payment) via the Admin wallet top-up
 * endpoint. There is no payment gateway and no webhook on this path.
 */
public record CreateTopupRequestRequest(

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount,

        @Size(max = 50)
        String paymentMethod,

        /** Optional proof-of-payment reference the vendor can quote (bank/ABA/KHQR txn id). */
        @Size(max = 255)
        String bankReference

) {}
