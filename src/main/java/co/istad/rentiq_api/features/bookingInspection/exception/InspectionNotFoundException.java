package co.istad.rentiq_api.features.bookingInspection.exception;

import java.util.UUID;

public class InspectionNotFoundException extends RuntimeException {
    public InspectionNotFoundException(UUID bookingId) {
        super("No inspection found for booking " + bookingId);
    }
}