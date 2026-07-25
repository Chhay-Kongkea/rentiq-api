package co.istad.rentiq_api.features.booking.exception;

public class InvalidBookingStatusTransitionException extends RuntimeException {
    public InvalidBookingStatusTransitionException(String from, String to) {
        super("Cannot transition booking from " + from + " to " + to + " for this role");
    }
}