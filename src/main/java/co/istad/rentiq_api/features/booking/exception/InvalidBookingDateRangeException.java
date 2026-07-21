package co.istad.rentiq_api.features.booking.exception;

public class InvalidBookingDateRangeException extends RuntimeException {
    public InvalidBookingDateRangeException(String message) {
        super(message);
    }
}