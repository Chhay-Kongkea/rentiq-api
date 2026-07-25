package co.istad.rentiq_api.features.item.exception;

public class InvalidImageException extends RuntimeException {
  public InvalidImageException(String message) {
    super(message);
  }
}
