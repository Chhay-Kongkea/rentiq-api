package co.istad.rentiq_api.exception;

public class InvalidItemOperationException extends RuntimeException {
    public InvalidItemOperationException(String message) {
        super(message);
    }
}
