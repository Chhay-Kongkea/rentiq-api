package co.istad.rentiq_api.features.review.exception;

import java.util.UUID;

public class ReviewImageNotFoundException extends RuntimeException {
    public ReviewImageNotFoundException(UUID id) {
        super("Review image " + id + " not found");
    }
}