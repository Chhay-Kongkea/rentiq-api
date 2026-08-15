package co.istad.rentiq_api.features.financialReport.dto.response;

import co.istad.rentiq_api.features.wallet.enums.TransactionDirection;
import co.istad.rentiq_api.features.wallet.enums.TransactionType;

import java.math.BigDecimal;

public record TransactionTypeSummaryRow(
        TransactionType transactionType,
        TransactionDirection direction,
        BigDecimal totalAmount,
        long transactionCount
) {}
