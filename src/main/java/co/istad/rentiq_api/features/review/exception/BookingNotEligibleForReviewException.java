package co.istad.rentiq_api.features.review.exception;

import java.util.UUID;

public class BookingNotEligibleForReviewException extends RuntimeException {
    public BookingNotEligibleForReviewException(UUID bookingId) {
        super("Booking " + bookingId + " must be COMPLETED before it can be reviewed");
    }
}