package co.istad.rentiq_api.features.report.controller;

import co.istad.rentiq_api.features.item.dto.respone.PageResponse;
import co.istad.rentiq_api.features.report.dto.request.CreateReportActionRequest;
import co.istad.rentiq_api.features.report.dto.request.UpdateReportStatusRequest;
import co.istad.rentiq_api.features.report.dto.response.ReportActionResponse;
import co.istad.rentiq_api.features.report.dto.response.ReportResponse;
import co.istad.rentiq_api.features.report.enums.ReportStatus;
import co.istad.rentiq_api.features.report.enums.ReportType;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping
    public PageResponse<ReportResponse> getAllReports(

            @RequestParam(required = false)
            ReportStatus status,

            @RequestParam(required = false)
            ReportType reportType,

            @RequestParam(required = false)
            String reporterId,

            @RequestParam(defaultValue = "0")
            @Min(0)
            Integer pageNumber,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            Integer pageSize,

            @RequestParam(defaultValue = "desc")
            String sortDirection
    ) {
        return reportService.getAllReports(
                status,
                reportType,
                reporterId,
                pageNumber,
                pageSize,
                sortDirection
        );
    }

    @GetMapping("/{reportId}")
    public ReportResponse getReport(@PathVariable UUID reportId) {
        return reportService.getReportForAdmin(reportId);
    }

    @PostMapping("/{reportId}/actions")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportActionResponse createAction(
            @PathVariable UUID reportId,

            @Valid
            @RequestBody
            CreateReportActionRequest request,

            Authentication authentication
    ) {
        return reportService.createAction(reportId, request, authentication.getName());
    }

    @GetMapping("/{reportId}/actions")
    public List<ReportActionResponse> getActions(@PathVariable UUID reportId) {
        return reportService.getReportActions(reportId);
    }

    @PatchMapping("/{reportId}/status")
    public ReportResponse updateStatus(
            @PathVariable UUID reportId,

            @Valid
            @RequestBody
            UpdateReportStatusRequest request
    ) {
        return reportService.updateStatus(reportId, request);
    }
}