package co.istad.rentiq_api.features.financialReport.controller;

import co.istad.rentiq_api.features.financialReport.dto.GroupBy;
import co.istad.rentiq_api.features.financialReport.dto.response.VendorBookingValueReportResponse;
import co.istad.rentiq_api.features.financialReport.service.FinancialReportService;
import co.istad.rentiq_api.security.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * A Vendor's own COMPLETED booking value (marketplace rental GMV) — never Vendor wallet
 * earnings, never Rentiq Platform Revenue. Rental payment is P2P: the renter pays the Vendor
 * directly, outside Rentiq, so Rentiq has no record of money actually received. The former
 * {@code /earnings} path (backed by an always-zero WalletTransaction query — no code ever
 * creates a booking-linked wallet transaction) has been removed pre-freeze; there was no
 * backend client dependency on it.
 */
@RestController
@RequestMapping("/api/v1/vendors/me/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class VendorFinancialReportController {

    private final FinancialReportService financialReportService;

    @GetMapping("/booking-value")
    public VendorBookingValueReportResponse getBookingValueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") GroupBy groupBy) {
        String ownerId = AuthUtils.extractUserId();
        return financialReportService.getVendorBookingValueReport(ownerId, from, to, groupBy);
    }
}
