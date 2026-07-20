package co.istad.rentiq_api.exception;

public class ItemRequestAccessDeniedException
        extends RuntimeException {

    public ItemRequestAccessDeniedException() {
        super("You do not have permission to modify this item request");
    }
}