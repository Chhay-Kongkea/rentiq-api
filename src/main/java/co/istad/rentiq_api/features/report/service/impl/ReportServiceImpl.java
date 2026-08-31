package co.istad.rentiq_api.features.report.service.impl;

import co.istad.rentiq_api.common.exception.InvalidOperationException;
import co.istad.rentiq_api.common.exception.InvalidStateException;
import co.istad.rentiq_api.common.exception.NotFoundException;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
import co.istad.rentiq_api.features.item.dto.respone.PageResponse;
import co.istad.rentiq_api.features.notification.enums.NotificationReferenceType;
import co.istad.rentiq_api.features.notification.enums.NotificationType;
import co.istad.rentiq_api.features.notification.service.NotificationService;
import co.istad.rentiq_api.features.report.dto.request.CreateReportActionRequest;
import co.istad.rentiq_api.features.report.dto.request.CreateReportRequest;
import co.istad.rentiq_api.features.report.dto.request.UpdateReportStatusRequest;
import co.istad.rentiq_api.features.report.dto.response.ReportActionResponse;
import co.istad.rentiq_api.features.report.dto.response.ReportResponse;
import co.istad.rentiq_api.features.report.entity.Report;
import co.istad.rentiq_api.features.report.entity.ReportAction;
import co.istad.rentiq_api.features.report.enums.ReportActionType;
import co.istad.rentiq_api.features.report.enums.ReportStatus;
import co.istad.rentiq_api.features.report.enums.ReportType;
import co.istad.rentiq_api.features.report.mapper.ReportActionMapper;
import co.istad.rentiq_api.features.report.mapper.ReportMapper;
import co.istad.rentiq_api.features.report.repository.ReportActionRepository;
import co.istad.rentiq_api.features.report.repository.ReportRepository;
import co.istad.rentiq_api.features.report.service.ReportService;
import co.istad.rentiq_api.features.report.specification.ReportSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ReportRepository reportRepository;
    private final ReportActionRepository reportActionRepository;
    private final ReportMapper reportMapper;
    private final ReportActionMapper reportActionMapper;
    private final AdminAuditService adminAuditService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public ReportResponse createReport(CreateReportRequest request, String authenticatedUserId) {
        validateReporter(request, authenticatedUserId);
        Report report = reportMapper.toEntity(request, authenticatedUserId);

        Report savedReport = reportRepository.save(report);

        return reportMapper.toResponse(savedReport);
    }

    @Override
    public PageResponse<ReportResponse> getMyReports(String authenticatedUserId, Integer pageNumber, Integer pageSize) {
        Pageable pageable = PageRequest.of(
                normalizePageNumber(pageNumber),
                normalizePageSize(pageSize),
                Sort.by("createdAt").descending()
        );

        Page<ReportResponse> response = reportRepository
                        .findAllByReporterIdOrderByCreatedAtDesc(
                                authenticatedUserId,
                                pageable
                        )
                        .map(reportMapper::toResponse);

        return PageResponse.from(response);
    }

    @Override
    public ReportResponse getMyReport(UUID reportId, String authenticatedUserId) {
        Report report = reportRepository
                .findByIdAndReporterId(reportId, authenticatedUserId)
                .orElseThrow(
                        () -> new NotFoundException("Report", reportId)
                );

        return reportMapper.toResponse(report);
    }

    @Override
    public PageResponse<ReportResponse> getAllReports(
            ReportStatus status,
            ReportType reportType,
            String reporterId,
            Integer pageNumber,
            Integer pageSize,
            String sortDirection
    ) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(
                normalizePageNumber(pageNumber),
                normalizePageSize(pageSize),
                Sort.by(direction, "createdAt")
        );

        Page<ReportResponse> response = reportRepository
                        .findAll(
                                ReportSpecification.adminFilter(status, reportType, reporterId),
                                pageable
                        )
                        .map(reportMapper::toResponse);

        return PageResponse.from(response);
    }

    @Override
    public ReportResponse getReportForAdmin(UUID reportId) {
        return reportMapper.toResponse(
                findReport(reportId)
        );
    }

    @Override
    @Transactional
    public ReportActionResponse createAction(UUID reportId, CreateReportActionRequest request, String authenticatedAdminId) {
        Report report = findReport(reportId);

        if (report.getStatus()
                == ReportStatus.DISMISSED) {
            throw new InvalidStateException(
                    "Report",
                    report.getStatus(),
                    "Actions cannot be added to a DISMISSED report"
            );
        }

        ReportStatus previousStatus = report.getStatus();

        ReportAction action = reportActionMapper.toEntity(
                        request,
                        report,
                        authenticatedAdminId
                );

        ReportAction savedAction = reportActionRepository.save(action);

        updateStatusAfterAction(report, request.actionType());

        adminAuditService.record(
                AdminAuditAction.MODERATION_ACTION_CREATED,
                AdminAuditTargetType.REPORT,
                reportId.toString(),
                null,
                Map.of("actionType", request.actionType().name()),
                request.notes());

        notifyReporterIfResolved(report, previousStatus);

        return reportActionMapper.toResponse(savedAction);
    }

    @Override
    public List<ReportActionResponse> getReportActions(UUID reportId) {
        findReport(reportId);

        return reportActionRepository
                .findAllByReportIdOrderByCreatedAtDesc(reportId)
                .stream()
                .map(reportActionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ReportResponse updateStatus(UUID reportId, UpdateReportStatusRequest request) {
        Report report = findReport(reportId);

        validateStatusTransition(
                report.getStatus(),
                request.status()
        );

        ReportStatus previousStatus = report.getStatus();

        report.setStatus(request.status());
        Report savedReport = reportRepository.save(report);

        adminAuditService.record(
                AdminAuditAction.REPORT_STATUS_CHANGED,
                AdminAuditTargetType.REPORT,
                reportId.toString(),
                Map.of("status", previousStatus.name()),
                Map.of("status", request.status().name()),
                null);

        notifyReporterIfResolved(savedReport, previousStatus);

        return reportMapper.toResponse(savedReport);
    }

    private void notifyReporterIfResolved(Report report, ReportStatus previousStatus) {
        if (report.getStatus() == previousStatus) {
            return;
        }
        if (report.getStatus() != ReportStatus.RESOLVED && report.getStatus() != ReportStatus.DISMISSED) {
            return;
        }

        notificationService.notifyUser(
                report.getReporterId(),
                NotificationType.REPORT,
                report.getStatus() == ReportStatus.RESOLVED ? "Report resolved" : "Report dismissed",
                "Your report has been reviewed. Status: " + report.getStatus().name() + ".",
                NotificationReferenceType.REPORT,
                report.getId());
    }

    private Report findReport(UUID reportId) {
        return reportRepository
                .findById(reportId)
                .orElseThrow(
                        () -> new NotFoundException("Report", reportId)
                );
    }

    private void validateReporter(CreateReportRequest request, String authenticatedUserId) {
        if (authenticatedUserId == null || authenticatedUserId.isBlank()) {
            throw new InvalidOperationException(
                    "Report", "An authenticated user is required"
            );
        }

        if (request.reportType() == ReportType.USER && authenticatedUserId.equals(
                request.reportedUserId()
        )) {
            throw new InvalidOperationException(
                    "Report", "A user cannot report their own account"
            );
        }
    }

    private void updateStatusAfterAction(Report report, ReportActionType actionType) {
        ReportStatus nextStatus =
                switch (actionType) {
                    case WARNING ->
                            ReportStatus.UNDER_REVIEW;

                    case SUSPENSION,
                         BAN,
                         ITEM_REMOVAL,
                         REVIEW_REMOVAL,
                         NO_ACTION ->
                            ReportStatus.RESOLVED;
                };

        report.setStatus(nextStatus);
        reportRepository.save(report);
    }

    private void validateStatusTransition(ReportStatus currentStatus, ReportStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        if (currentStatus == ReportStatus.RESOLVED && newStatus == ReportStatus.OPEN) {
            throw new InvalidStateException(
                    "Report",
                    currentStatus,
                    "A RESOLVED report cannot be reopened directly"
            );
        }

        if (currentStatus == ReportStatus.DISMISSED && newStatus != ReportStatus.UNDER_REVIEW) {
            throw new InvalidStateException(
                    "Report",
                    currentStatus,
                    "A DISMISSED report can only move to UNDER_REVIEW"
            );
        }
    }

    private int normalizePageNumber(Integer pageNumber) {
        return pageNumber == null
                ? 0 : Math.max(pageNumber, 0);
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.max(1, Math.min(pageSize, MAX_PAGE_SIZE));
    }
}
