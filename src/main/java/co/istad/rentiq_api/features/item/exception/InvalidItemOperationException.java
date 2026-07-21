package co.istad.rentiq_api.features.item.exception;

public class InvalidItemOperationException extends RuntimeException {
    public InvalidItemOperationException(String message) {
        super(message);
    }
}
