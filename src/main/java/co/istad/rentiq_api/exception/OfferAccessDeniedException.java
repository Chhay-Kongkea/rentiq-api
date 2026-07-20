package co.istad.rentiq_api.exception;

public class OfferAccessDeniedException
        extends RuntimeException {

    public OfferAccessDeniedException() {
        super("You do not have permission to modify this offer");
    }
}
