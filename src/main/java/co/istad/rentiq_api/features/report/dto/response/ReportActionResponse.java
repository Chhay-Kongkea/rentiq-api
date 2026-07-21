package co.istad.rentiq_api.features.report.dto.response;

import co.istad.rentiq_api.features.report.enums.ReportActionType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReportActionResponse(
        UUID id,
        UUID reportId,
        String adminId,
        ReportActionType actionType,
        String notes,
        OffsetDateTime createdAt
) {
}