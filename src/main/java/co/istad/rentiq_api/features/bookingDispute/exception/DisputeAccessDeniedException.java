package co.istad.rentiq_api.features.bookingDispute.exception;

public class DisputeAccessDeniedException extends RuntimeException {
    public DisputeAccessDeniedException(String message) {
        super(message);
    }
}