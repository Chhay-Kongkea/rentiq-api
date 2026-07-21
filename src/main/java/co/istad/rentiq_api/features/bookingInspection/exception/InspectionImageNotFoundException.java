package co.istad.rentiq_api.features.bookingInspection.exception;

import java.util.UUID;

public class InspectionImageNotFoundException extends RuntimeException {
    public InspectionImageNotFoundException(UUID id) {
        super("Inspection image " + id + " not found");
    }
}