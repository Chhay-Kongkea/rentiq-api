package co.istad.rentiq_api.features.wallet.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminWalletTopupResponse(
        UUID walletId,
        String ownerId,
        BigDecimal amount,
        String currency,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        UUID transactionId,
        OffsetDateTime createdAt
) {}
