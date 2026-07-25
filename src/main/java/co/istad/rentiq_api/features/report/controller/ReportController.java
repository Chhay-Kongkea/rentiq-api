package co.istad.rentiq_api.features.report.controller;

import co.istad.rentiq_api.features.item.dto.respone.PageResponse;
import co.istad.rentiq_api.features.report.dto.request.CreateReportRequest;
import co.istad.rentiq_api.features.report.dto.response.ReportResponse;
import co.istad.rentiq_api.features.report.service.ReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('USER')")
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse createReport(@Valid @RequestBody CreateReportRequest request, Authentication authentication) {
        return reportService.createReport(
                request, authentication.getName()
        );
    }

    @GetMapping("/me")
    public PageResponse<ReportResponse> getMyReports(
            Authentication authentication,

            @RequestParam(defaultValue = "0")
            @Min(0)
            Integer pageNumber,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            Integer pageSize
    ) {
        return reportService.getMyReports(authentication.getName(), pageNumber, pageSize);
    }

    @GetMapping("/{reportId}")
    public ReportResponse getMyReport(@PathVariable UUID reportId, Authentication authentication) {
        return reportService.getMyReport(reportId, authentication.getName());
    }
}