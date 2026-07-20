package co.istad.rentiq_api.exception;

import java.util.UUID;

public class OfferNotFoundException
        extends RuntimeException {

    public OfferNotFoundException(UUID id) {
        super("Offer not found with ID: " + id);
    }
}
