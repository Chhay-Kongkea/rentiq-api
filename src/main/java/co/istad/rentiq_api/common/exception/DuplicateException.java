package co.istad.rentiq_api.common.exception;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class DuplicateException extends RuntimeException {

    private final Map<String, Object> details;

    public DuplicateException(String resourceName, String message) {
        super(message);

        this.details = Map.of("resourceName", resourceName);
    }

    public DuplicateException(String resourceName, String fieldName, Object fieldValue) {
        super(
                "%s already exists with %s: %s"
                        .formatted(resourceName, fieldName, fieldValue)
        );

        Map<String, Object> errorDetails = new LinkedHashMap<>();

        errorDetails.put("resourceName", resourceName);
        errorDetails.put("fieldName", fieldName);
        errorDetails.put("fieldValue", String.valueOf(fieldValue));

        this.details = Map.copyOf(errorDetails);
    }

    public DuplicateException(String message) {
        super(message);
        this.details = Map.of();
    }
}