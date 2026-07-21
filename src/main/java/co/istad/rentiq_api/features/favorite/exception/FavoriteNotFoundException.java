package co.istad.rentiq_api.features.favorite.exception;

import java.util.UUID;

public class FavoriteNotFoundException extends RuntimeException {
    public FavoriteNotFoundException(UUID itemId) {
        super("Item " + itemId + " is not in your favorites");
    }
}