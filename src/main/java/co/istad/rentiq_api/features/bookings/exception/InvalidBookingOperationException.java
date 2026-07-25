package co.istad.rentiq_api.features.bookings.exception;

public class InvalidBookingOperationException extends RuntimeException {

    public InvalidBookingOperationException(String message) {
        super(message);
    }
}
