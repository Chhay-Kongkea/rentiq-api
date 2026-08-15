package co.istad.rentiq_api.features.bookings.exception;

public class BookingAccessDeniedException extends RuntimeException {

    public BookingAccessDeniedException() {
        super("You do not have permission to access this booking");
    }

    public BookingAccessDeniedException(String message) {
        super(message);
    }
}
