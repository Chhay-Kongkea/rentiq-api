package co.istad.rentiq_api.features.review.exception;

import java.util.UUID;

public class ReviewAlreadyExistsException extends RuntimeException {
    public ReviewAlreadyExistsException(UUID bookingId) {
        super("Booking " + bookingId + " has already been reviewed");
    }
}