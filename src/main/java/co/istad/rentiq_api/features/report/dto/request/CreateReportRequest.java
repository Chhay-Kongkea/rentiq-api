package co.istad.rentiq_api.features.report.dto.request;

import co.istad.rentiq_api.features.report.enums.ReportType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateReportRequest(

        @NotNull(message = "Report type is required")
        ReportType reportType,
        String reportedUserId,
        UUID reportedItemId,
        UUID reportedReviewId,

        @Size(max = 5000, message = "Description cannot exceed 5000 characters")
        String description

) {

    @AssertTrue(message = "Exactly one report target must be provided")
    public boolean isExactlyOneTargetProvided() {
        int targetCount = 0;

        if (reportedUserId != null && !reportedUserId.isBlank()) {
            targetCount++;
        }

        if (reportedItemId != null) {
            targetCount++;
        }

        if (reportedReviewId != null) {
            targetCount++;
        }

        return targetCount == 1;
    }

    @AssertTrue(message = "Report type must match the provided target")
    public boolean isReportTypeValid() {
        if (reportType == null) {
            return true;
        }

        return switch (reportType) {
            case USER ->
                    reportedUserId != null
                            && !reportedUserId.isBlank()
                            && reportedItemId == null
                            && reportedReviewId == null;

            case ITEM ->
                    reportedItemId != null
                            && reportedUserId == null
                            && reportedReviewId == null;

            case REVIEW ->
                    reportedReviewId != null
                            && reportedUserId == null
                            && reportedItemId == null;
        };
    }
}