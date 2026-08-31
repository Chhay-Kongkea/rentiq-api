package co.istad.rentiq_api.features.promotion.dto.request;

import co.istad.rentiq_api.features.promotion.enums.PromotionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * status is part of the contract but is deliberately restricted to SUSPENDED in the service —
 * this endpoint is a moderation action, not a general status-setter. Any other value (ACTIVE,
 * EXPIRED, CANCELLED) is rejected server-side.
 */
public record SuspendPromotionRequest(

        @NotNull
        PromotionStatus status,

        @NotBlank
        @Size(max = 1000)
        String reason

) {}
