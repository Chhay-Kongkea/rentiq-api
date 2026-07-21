package co.istad.rentiq_api.features.bookingDispute.exception;

import java.util.UUID;

public class DisputeNotFoundException extends RuntimeException {
    public DisputeNotFoundException(UUID id) {
        super("Dispute " + id + " not found");
    }
}