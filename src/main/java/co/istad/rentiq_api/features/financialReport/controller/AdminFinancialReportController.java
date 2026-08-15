package co.istad.rentiq_api.features.financialReport.controller;

import co.istad.rentiq_api.features.financialReport.dto.GroupBy;
import co.istad.rentiq_api.features.financialReport.dto.response.CommissionTimeSeriesResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.RevenueReportResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.TransactionReportResponse;
import co.istad.rentiq_api.features.financialReport.service.FinancialReportService;
import co.istad.rentiq_api.security.AuthUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminFinancialReportController {

    private static final MediaType XLSX_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final FinancialReportService financialReportService;

    @GetMapping("/revenue")
    public RevenueReportResponse getRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") GroupBy groupBy,
            @PageableDefault(size = 50) Pageable pageable) {
        return financialReportService.getRevenueReport(from, to, groupBy, pageable);
    }

    @GetMapping("/commission")
    public CommissionTimeSeriesResponse getCommissionReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") GroupBy groupBy,
            @PageableDefault(size = 50) Pageable pageable) {
        return financialReportService.getCommissionReport(from, to, groupBy, pageable);
    }

    @GetMapping("/transactions")
    public TransactionReportResponse getTransactionReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") GroupBy groupBy,
            @PageableDefault(size = 100) Pageable pageable) {
        return financialReportService.getTransactionReport(from, to, groupBy, pageable);
    }

    @GetMapping(value = "/revenue/export.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportRevenuePdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") GroupBy groupBy) {
        byte[] pdf = financialReportService.exportRevenuePdf(from, to, groupBy);

        String adminId = AuthUtils.extractUserId();
        log.info("Admin {} exported revenue report PDF (from={}, to={}, groupBy={})", adminId, from, to, groupBy);
        String filename = "revenue-report-" + from + "_to_" + (to != null ? to : LocalDate.now()) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/revenue/export.xlsx")
    public ResponseEntity<byte[]> exportRevenueXlsx(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") GroupBy groupBy) {
        byte[] xlsx = financialReportService.exportRevenueXlsx(from, to, groupBy);

        String adminId = AuthUtils.extractUserId();
        log.info("Admin {} exported revenue report xlsx (from={}, to={}, groupBy={})", adminId, from, to, groupBy);

        String filename = "revenue-report-" + from + "_to_" + (to != null ? to : LocalDate.now()) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(XLSX_MEDIA_TYPE)
                .body(xlsx);
    }
}
