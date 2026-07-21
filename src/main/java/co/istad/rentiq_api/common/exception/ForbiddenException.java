package co.istad.rentiq_api.common.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class ForbiddenException extends RuntimeException {

    private final Map<String, Object> details;

    public ForbiddenException(String resourceName) {
        super("You do not have permission to access this %s"
                        .formatted(resourceName.toLowerCase())
        );

        this.details = Map.of("resourceName", resourceName);
    }

    public ForbiddenException(String resourceName, String message) {
        super(message);
        this.details = Map.of("resourceName", resourceName);
    }

//    public ForbiddenException(String message) {
//        super(message);
//        this.details = Map.of();
//    }
}