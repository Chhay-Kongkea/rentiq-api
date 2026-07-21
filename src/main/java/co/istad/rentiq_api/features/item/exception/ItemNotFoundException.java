package co.istad.rentiq_api.exception;


import java.util.UUID;

public class ItemNotFoundException extends RuntimeException {

    public ItemNotFoundException(UUID itemId) {
        super("Item not found with ID: " + itemId);
    }
}
