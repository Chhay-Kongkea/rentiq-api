package co.istad.rentiq_api.features.bookingDispute.exception;

import java.util.UUID;

public class DisputeBookingNotFoundException extends RuntimeException {
    public DisputeBookingNotFoundException(UUID bookingId) {
        super("Booking " + bookingId + " not found");
    }
}