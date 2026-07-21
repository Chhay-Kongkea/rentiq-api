package co.istad.rentiq_api.features.report.dto.request;

import co.istad.rentiq_api.features.report.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateReportStatusRequest(

        @NotNull(message = "Report status is required")
        ReportStatus status

) {
}