package co.istad.rentiq_api.features.financialReport.controller;

import co.istad.rentiq_api.features.financialReport.dto.GroupBy;
import co.istad.rentiq_api.features.financialReport.dto.response.VendorEarningsReportResponse;
import co.istad.rentiq_api.features.financialReport.service.FinancialReportService;
import co.istad.rentiq_api.security.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/vendors/me/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class VendorFinancialReportController {

    private final FinancialReportService financialReportService;

    @GetMapping("/earnings")
    public VendorEarningsReportResponse getEarningsReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") GroupBy groupBy,
            @PageableDefault(size = 50) Pageable pageable) {
        String ownerId = AuthUtils.extractUserId();
        return financialReportService.getVendorEarningsReport(ownerId, from, to, groupBy, pageable);
    }
}
