package co.istad.rentiq_api.features.financialReport.service;

import co.istad.rentiq_api.features.financialReport.dto.GroupBy;
import co.istad.rentiq_api.features.financialReport.dto.response.CommissionTimeSeriesResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.RevenueReportResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.TransactionReportResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.VendorEarningsReportResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface FinancialReportService {

    RevenueReportResponse getRevenueReport(LocalDate from, LocalDate to, GroupBy groupBy, Pageable pageable);

    CommissionTimeSeriesResponse getCommissionReport(LocalDate from, LocalDate to, GroupBy groupBy, Pageable pageable);

    TransactionReportResponse getTransactionReport(LocalDate from, LocalDate to, GroupBy groupBy, Pageable pageable);

    VendorEarningsReportResponse getVendorEarningsReport(String ownerId, LocalDate from, LocalDate to, GroupBy groupBy, Pageable pageable);

    byte[] exportRevenuePdf(LocalDate from, LocalDate to, GroupBy groupBy);

    byte[] exportRevenueXlsx(LocalDate from, LocalDate to, GroupBy groupBy);
}
