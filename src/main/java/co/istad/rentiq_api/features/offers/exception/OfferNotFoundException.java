package co.istad.rentiq_api.features.offers.exception;

import java.util.UUID;

public class OfferNotFoundException extends RuntimeException {

    public OfferNotFoundException(UUID id) {
        super("Offer not found with id: " + id);
    }
}