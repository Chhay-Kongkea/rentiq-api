package co.istad.rentiq_api.features.report.dto.request;

import co.istad.rentiq_api.features.report.enums.ReportActionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReportActionRequest(

        @NotNull(message = "Action type is required")
        ReportActionType actionType,

        @Size(max = 5000, message = "Notes cannot exceed 5000 characters")
        String notes

) {
}