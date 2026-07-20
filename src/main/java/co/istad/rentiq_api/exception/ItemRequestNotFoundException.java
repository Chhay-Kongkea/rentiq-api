package co.istad.rentiq_api.exception;

import java.util.UUID;

public class ItemRequestNotFoundException
        extends RuntimeException {

    public ItemRequestNotFoundException(UUID id) {
        super("Item request not found with ID: " + id);
    }
}