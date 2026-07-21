package co.istad.rentiq_api.features.favorite.exception;

import java.util.UUID;

public class FavoriteItemNotFoundException extends RuntimeException {
    public FavoriteItemNotFoundException(UUID itemId) {
        super("Item " + itemId + " does not exist");
    }
}