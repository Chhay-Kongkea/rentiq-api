package co.istad.rentiq_api.features.item.exception;

import java.util.UUID;

public class AvailabilityBlockNotFoundException extends RuntimeException {

    public AvailabilityBlockNotFoundException(UUID blockId) {
        super("Availability block not found with ID: " + blockId);
    }
}
