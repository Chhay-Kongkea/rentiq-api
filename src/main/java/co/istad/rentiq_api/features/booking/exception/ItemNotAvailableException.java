package co.istad.rentiq_api.features.booking.exception;

import java.util.UUID;

public class ItemNotAvailableException extends RuntimeException {
    public ItemNotAvailableException(UUID itemId) {
        super("Item " + itemId + " is not available for the selected dates");
    }
}