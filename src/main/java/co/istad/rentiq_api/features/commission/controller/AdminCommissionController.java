package co.istad.rentiq_api.features.commission.controller;

import co.istad.rentiq_api.features.commission.dto.request.UpdateCommissionRateRequest;
import co.istad.rentiq_api.features.commission.dto.response.CommissionRateResponse;
import co.istad.rentiq_api.features.commission.dto.response.CommissionReportResponse;
import co.istad.rentiq_api.features.commission.service.CommissionService;
import co.istad.rentiq_api.security.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/commissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommissionController {

    private final CommissionService commissionService;

    @GetMapping
    public List<CommissionRateResponse> listCommissionRates() {
        return commissionService.listCommissionRates();
    }

    @PatchMapping("/{categoryId}")
    public CommissionRateResponse updateCommissionRate(
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateCommissionRateRequest request
    ) {
        return commissionService.updateCommissionRate(categoryId, request, AuthUtils.extractUserId());
    }

    @GetMapping("/report")
    public CommissionReportResponse getCommissionReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return commissionService.getCommissionReport(from, to);
    }
}
