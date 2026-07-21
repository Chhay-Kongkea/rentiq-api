package co.istad.rentiq_api.common.exception;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class NotFoundException extends RuntimeException {

    private final Map<String, Object> details;

    public NotFoundException(String resourceName, Object resourceId) {
        super(
                "%s not found with ID: %s"
                        .formatted(resourceName, resourceId)
        );

        this.details = Map.of("resourceName", resourceName, "resourceId", String.valueOf(resourceId));
    }

    public NotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(
                "%s not found with %s: %s"
                        .formatted(resourceName, fieldName, fieldValue)
        );

        Map<String, Object> errorDetails = new LinkedHashMap<>();

        errorDetails.put("resourceName", resourceName);
        errorDetails.put("fieldName", fieldName);
        errorDetails.put("fieldValue", String.valueOf(fieldValue));

        this.details = Map.copyOf(errorDetails);
    }

    public NotFoundException(String message) {
        super(message);
        this.details = Map.of();
    }
}