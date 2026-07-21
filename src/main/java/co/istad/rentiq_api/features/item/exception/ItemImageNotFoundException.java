package co.istad.rentiq_api.features.item.exception;

import java.util.UUID;

public class ItemImageNotFoundException extends RuntimeException {

  public ItemImageNotFoundException(UUID imageId) {
    super("Item image not found with ID: " + imageId);
  }
}
