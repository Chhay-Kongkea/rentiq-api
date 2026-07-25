package co.istad.rentiq_api.features.review.exception;

import java.util.UUID;

public class VendorReplyNotFoundException extends RuntimeException {
    public VendorReplyNotFoundException(UUID reviewId) {
        super("Review " + reviewId + " has no vendor reply yet");
    }
}