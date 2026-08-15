package co.istad.rentiq_api.features.financialReport.dto.response;

import co.istad.rentiq_api.features.financialReport.dto.GroupBy;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public record TransactionReportResponse(
        LocalDate from,
        LocalDate to,
        GroupBy groupBy,
        List<TransactionTypeSummaryRow> summary,
        Page<TransactionPeriodRow> rows
) {}
