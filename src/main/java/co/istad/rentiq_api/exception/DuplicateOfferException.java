package co.istad.rentiq_api.exception;

public class DuplicateOfferException
        extends RuntimeException {

    public DuplicateOfferException() {
        super("You have already submitted an offer for this request");
    }
}
