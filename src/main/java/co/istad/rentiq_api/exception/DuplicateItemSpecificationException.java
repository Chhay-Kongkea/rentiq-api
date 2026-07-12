package co.istad.rentiq_api.exception;

public class DuplicateItemSpecificationException extends RuntimeException {

  public DuplicateItemSpecificationException(String key) {
    super("Duplicate item specification key: " + key);
  }
}
