package co.istad.rentiq_api.exception;

public class IllegalItemRequestStateException
        extends RuntimeException {

    public IllegalItemRequestStateException(String message) {
        super(message);
    }
}
