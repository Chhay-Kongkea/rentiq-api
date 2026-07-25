package co.istad.rentiq_api.features.report.service;

import co.istad.rentiq_api.features.item.dto.respone.PageResponse;
import co.istad.rentiq_api.features.report.dto.request.CreateReportActionRequest;
import co.istad.rentiq_api.features.report.dto.request.CreateReportRequest;
import co.istad.rentiq_api.features.report.dto.request.UpdateReportStatusRequest;
import co.istad.rentiq_api.features.report.dto.response.ReportActionResponse;
import co.istad.rentiq_api.features.report.dto.response.ReportResponse;
import co.istad.rentiq_api.features.report.enums.ReportStatus;
import co.istad.rentiq_api.features.report.enums.ReportType;

import java.util.List;
import java.util.UUID;

public interface ReportService {

    ReportResponse createReport(CreateReportRequest request, String authenticatedUserId);
    PageResponse<ReportResponse> getMyReports(String authenticatedUserId, Integer pageNumber, Integer pageSize);
    ReportResponse getMyReport(UUID reportId, String authenticatedUserId);
    PageResponse<ReportResponse> getAllReports(
            ReportStatus status,
            ReportType reportType,
            String reporterId,
            Integer pageNumber,
            Integer pageSize,
            String sortDirection
    );

    ReportResponse getReportForAdmin(UUID reportId);
    ReportActionResponse createAction(UUID reportId, CreateReportActionRequest request, String authenticatedAdminId);

    List<ReportActionResponse> getReportActions(UUID reportId);
    ReportResponse updateStatus(UUID reportId, UpdateReportStatusRequest request);
}