package co.istad.rentiq_api.exception;

public class IllegalOfferStateException
        extends RuntimeException {

    public IllegalOfferStateException(String message) {
        super(message);
    }
}