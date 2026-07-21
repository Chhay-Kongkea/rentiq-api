package co.istad.rentiq_api.features.bookingInspection.exception;

public class InspectionAccessDeniedException extends RuntimeException {
    public InspectionAccessDeniedException(String message) {
        super(message);
    }
}