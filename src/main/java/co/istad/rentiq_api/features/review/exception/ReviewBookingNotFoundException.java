package co.istad.rentiq_api.features.review.exception;

import java.util.UUID;

public class ReviewBookingNotFoundException extends RuntimeException {
    public ReviewBookingNotFoundException(UUID bookingId) {
        super("Booking " + bookingId + " not found");
    }
}