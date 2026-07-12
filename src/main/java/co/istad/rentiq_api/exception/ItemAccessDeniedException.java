package co.istad.rentiq_api.exception;

public class ItemAccessDeniedException extends RuntimeException {

    public ItemAccessDeniedException() {
        super("You do not have permission to manage this item");
    }

    public ItemAccessDeniedException(String message) {
        super(message);
    }
}
