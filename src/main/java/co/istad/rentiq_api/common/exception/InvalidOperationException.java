package co.istad.rentiq_api.common.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class InvalidOperationException extends RuntimeException {

    private final Map<String, Object> details;

    public InvalidOperationException(String resourceName, String message) {
        super(message);

        this.details = Map.of("resourceName", resourceName);
    }

    public InvalidOperationException(String message) {
        super(message);
        this.details = Map.of();
    }
}