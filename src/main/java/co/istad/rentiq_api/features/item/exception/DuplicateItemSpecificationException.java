package co.istad.rentiq_api.features.item.exception;

public class DuplicateItemSpecificationException extends RuntimeException {

  public DuplicateItemSpecificationException(String key) {
    super("Duplicate item specification key: " + key);
  }
}
