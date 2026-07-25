package co.istad.rentiq_api.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        int status,
        String error,
        String code,
        String message,
        String path,
        OffsetDateTime timestamp,
        Map<String, Object> details,
        Map<String, String> validationErrors

) {

    public static ApiErrorResponse of(
            int status,
            String error,
            String code,
            String message,
            String path,
            Map<String, Object> details
    ) {
        return new ApiErrorResponse(
                status,
                error,
                code,
                message,
                path,
                OffsetDateTime.now(),
                details == null || details.isEmpty()
                        ? null
                        : details,
                null
        );
    }

    public static ApiErrorResponse validation(
            int status,
            String error,
            String code,
            String message,
            String path,
            Map<String, String> validationErrors
    ) {
        return new ApiErrorResponse(
                status,
                error,
                code,
                message,
                path,
                OffsetDateTime.now(),
                null,
                validationErrors
        );
    }
}