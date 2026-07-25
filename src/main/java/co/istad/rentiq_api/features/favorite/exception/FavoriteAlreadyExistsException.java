package co.istad.rentiq_api.features.favorite.exception;

import java.util.UUID;

public class FavoriteAlreadyExistsException extends RuntimeException {
    public FavoriteAlreadyExistsException(UUID itemId) {
        super("Item " + itemId + " is already in your favorites");
    }
}