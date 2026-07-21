package co.istad.rentiq_api.features.report.dto.response;

import co.istad.rentiq_api.features.report.enums.ReportStatus;
import co.istad.rentiq_api.features.report.enums.ReportType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReportResponse(
        UUID id,
        String reporterId,
        String reportedUserId,
        UUID reportedItemId,
        UUID reportedReviewId,
        ReportType reportType,
        String description,
        ReportStatus status,
        int actionCount,
        OffsetDateTime createdAt
) {
}