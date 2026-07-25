package co.istad.rentiq_api.features.bookingInspection.exception;

import java.util.UUID;

public class InspectionBookingNotFoundException extends RuntimeException {
    public InspectionBookingNotFoundException(UUID bookingId) {
        super("Booking " + bookingId + " not found");
    }
}