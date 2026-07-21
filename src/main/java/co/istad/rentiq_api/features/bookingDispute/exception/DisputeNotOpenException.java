package co.istad.rentiq_api.features.bookingDispute.exception;

import java.util.UUID;

public class DisputeNotOpenException extends RuntimeException {
    public DisputeNotOpenException(UUID id) {
        super("Dispute " + id + " is no longer OPEN and cannot be edited");
    }
}