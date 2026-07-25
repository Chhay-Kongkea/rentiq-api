package co.istad.rentiq_api.features.bookingInspection.exception;

import java.util.UUID;

public class InspectionAlreadyExistsException extends RuntimeException {
    public InspectionAlreadyExistsException(UUID bookingId) {
        super("Booking " + bookingId + " already has an inspection");
    }
}