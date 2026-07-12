package co.istad.rentiq_api.features;


import co.istad.rentiq_api.exception.*;
import co.istad.rentiq_api.features.category.exception.CategoryNotFoundException;
import co.istad.rentiq_api.features.category.exception.DuplicateCategoryException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(
                ex.getStatusCode().value()
        );

        String message = ex.getReason() != null
                ? ex.getReason()
                : ex.getMessage();

        return buildError(status, message);
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<?> handleCategoryNotFound(CategoryNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateCategoryException.class)
    public ResponseEntity<?> handleDuplicateCategory(DuplicateCategoryException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<?> handleItemNotFound(ItemNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ItemAccessDeniedException.class)
    public ResponseEntity<?> handleItemAccessDenied(ItemAccessDeniedException ex) {
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(DuplicateItemSpecificationException.class)
    public ResponseEntity<?> handleDuplicateItemSpecification(DuplicateItemSpecificationException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidItemOperationException.class)
    public ResponseEntity<?> handleInvalidItemOperation(InvalidItemOperationException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        return buildError(
                HttpStatus.BAD_REQUEST,
                errors
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDBError(DataIntegrityViolationException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "Invalid data provided. Please check your input.");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private ResponseEntity<?> buildError(HttpStatus status, Object message) {
        Map<String, Object> error = new HashMap<>();

        error.put("status", status.value());
        error.put("error", status.getReasonPhrase());
        error.put("message", message);
        error.put("timestamp", Instant.now());

        return new ResponseEntity<>(error, status);
    }

    @ExceptionHandler(ItemImageNotFoundException.class)
    public ResponseEntity<?> handleItemImageNotFound(ItemImageNotFoundException exception) {
        return buildError(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<?> handleInvalidImage(InvalidImageException exception) {
        return buildError(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(ImageStorageException.class)
    public ResponseEntity<?> handleImageStorage(ImageStorageException exception) {
        return buildError(HttpStatus.BAD_GATEWAY, exception.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaximumUploadSize(MaxUploadSizeExceededException exception) {
        return buildError(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file or request is too large");
    }
}