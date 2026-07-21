package co.istad.rentiq_api.features.booking.exception;

import java.util.UUID;

public class BookingItemNotFoundException extends RuntimeException {
    public BookingItemNotFoundException(UUID itemId) {
        super("Item " + itemId + " not found");
    }
}