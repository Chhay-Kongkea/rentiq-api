package co.istad.rentiq_api.common.exception;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class InvalidStateException extends RuntimeException {

    private final Map<String, Object> details;

    public InvalidStateException(String resourceName, Object currentState, String message) {
        super(message);

        Map<String, Object> errorDetails = new LinkedHashMap<>();
        errorDetails.put("resourceName", resourceName);
        errorDetails.put("currentState", String.valueOf(currentState));

        this.details = Map.copyOf(errorDetails);
    }

    public InvalidStateException(String message) {
        super(message);
        this.details = Map.of();
    }
}