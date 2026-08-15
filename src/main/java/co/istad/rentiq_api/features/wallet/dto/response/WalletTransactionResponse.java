package co.istad.rentiq_api.features.wallet.dto.response;

import co.istad.rentiq_api.features.wallet.enums.TransactionDirection;
import co.istad.rentiq_api.features.wallet.enums.TransactionType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record WalletTransactionResponse(
        UUID id,
        UUID walletId,
        TransactionType transactionType,
        BigDecimal amount,
        TransactionDirection direction,
        BigDecimal balanceAfter,
        String description,
        UUID bookingId,
        UUID topupRequestId,
        UUID advertisementId,
        UUID promotionId,
        OffsetDateTime createdAt
) {}
