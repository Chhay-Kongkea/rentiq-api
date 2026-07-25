package co.istad.rentiq_api.features.booking.exception;

import java.util.UUID;

public class BookingNotFoundException extends RuntimeException {
    public BookingNotFoundException(UUID id) {
        super("Booking " + id + " not found");
    }
}