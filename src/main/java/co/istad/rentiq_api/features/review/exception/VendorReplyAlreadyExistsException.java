package co.istad.rentiq_api.features.review.exception;

import java.util.UUID;

public class VendorReplyAlreadyExistsException extends RuntimeException {
    public VendorReplyAlreadyExistsException(UUID reviewId) {
        super("Review " + reviewId + " already has a vendor reply");
    }
}