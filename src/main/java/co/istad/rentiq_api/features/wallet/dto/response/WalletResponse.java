package co.istad.rentiq_api.features.wallet.dto.response;

import co.istad.rentiq_api.features.wallet.enums.WalletStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record WalletResponse(
        UUID id,
        String ownerId,
        BigDecimal balance,
        BigDecimal frozenBalance,
        String currency,
        WalletStatus status,
        OffsetDateTime updatedAt
) {}
