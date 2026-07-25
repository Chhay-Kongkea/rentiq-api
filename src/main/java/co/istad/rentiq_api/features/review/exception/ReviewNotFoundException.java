package co.istad.rentiq_api.features.review.exception;

import java.util.UUID;

public class ReviewNotFoundException extends RuntimeException {
    public ReviewNotFoundException(UUID id) {
        super("Review " + id + " not found");
    }
}