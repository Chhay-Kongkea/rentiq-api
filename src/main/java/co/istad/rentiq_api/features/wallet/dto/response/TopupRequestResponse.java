package co.istad.rentiq_api.features.wallet.dto.response;

import co.istad.rentiq_api.features.wallet.enums.TopupStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record TopupRequestResponse(
        UUID id,
        UUID walletId,
        BigDecimal amount,
        String paymentMethod,
        TopupStatus status,
        String bankReference,
        OffsetDateTime createdAt
) {}
