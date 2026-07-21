package co.istad.rentiq_api.common.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class StorageException extends RuntimeException {

    private final Map<String, Object> details;

    public StorageException(String message) {
        super(message);
        this.details = Map.of();
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
        this.details = Map.of();
    }

    public StorageException(String storageProvider, String message, Throwable cause) {
        super(message, cause);

        this.details = Map.of(
                "storageProvider",
                storageProvider
        );
    }
}