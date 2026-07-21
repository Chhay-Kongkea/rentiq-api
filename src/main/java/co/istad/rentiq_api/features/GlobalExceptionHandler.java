//package co.istad.rentiq_api.features;
//
//import co.istad.rentiq_api.features.auth.exception.AuthException;
//import co.istad.rentiq_api.features.category.exception.CategoryNotFoundException;
//import co.istad.rentiq_api.features.category.exception.DuplicateCategoryException;
//import co.istad.rentiq_api.features.item.exception.*;
//import co.istad.rentiq_api.features.userProfile.exception.UserProfileException;
//import jakarta.validation.ConstraintViolationException;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.dao.DataIntegrityViolationException;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.validation.FieldError;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//import org.springframework.web.multipart.MaxUploadSizeExceededException;
//import org.springframework.web.server.ResponseStatusException;
//
//import java.time.Instant;
//import java.util.HashMap;
//import java.util.Map;
//
//@RestControllerAdvice
//@Slf4j
//public class GlobalExceptionHandler {
//
//    // ---------- Generic / framework exceptions ----------
//
//    @ExceptionHandler(ResponseStatusException.class)
//    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
//        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
//        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();
//        log.warn("ResponseStatusException: {}", message);
//        return buildError(status, message);
//    }
//
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
//        Map<String, String> fieldErrors = new HashMap<>();
//        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
//            fieldErrors.put(error.getField(), error.getDefaultMessage());
//        }
//        log.warn("Validation failed: {}", fieldErrors);
//        return buildErrorWithDetails(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
//    }
//
//    @ExceptionHandler(ConstraintViolationException.class)
//    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
//        log.warn("Constraint violation: {}", ex.getMessage());
//        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
//    }
//
//    @ExceptionHandler(DataIntegrityViolationException.class)
//    public ResponseEntity<Map<String, Object>> handleDBError(DataIntegrityViolationException ex) {
//        log.error("Data integrity violation", ex);
//        return buildError(HttpStatus.BAD_REQUEST, "Invalid data provided. Please check your input.");
//    }
//
//    @ExceptionHandler(MaxUploadSizeExceededException.class)
//    public ResponseEntity<Map<String, Object>> handleMaximumUploadSize(MaxUploadSizeExceededException ex) {
//        return buildError(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file or request is too large");
//    }
//
//    // ---------- Category ----------
//
//    @ExceptionHandler(CategoryNotFoundException.class)
//    public ResponseEntity<Map<String, Object>> handleCategoryNotFound(CategoryNotFoundException ex) {
//        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
//    }
//
//    @ExceptionHandler(DuplicateCategoryException.class)
//    public ResponseEntity<Map<String, Object>> handleDuplicateCategory(DuplicateCategoryException ex) {
//        return buildError(HttpStatus.CONFLICT, ex.getMessage());
//    }
//
//    // ---------- Item ----------
//
//    @ExceptionHandler(ItemNotFoundException.class)
//    public ResponseEntity<Map<String, Object>> handleItemNotFound(ItemNotFoundException ex) {
//        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
//    }
//
//    @ExceptionHandler(ItemAccessDeniedException.class)
//    public ResponseEntity<Map<String, Object>> handleItemAccessDenied(ItemAccessDeniedException ex) {
//        return buildError(HttpStatus.FORBIDDEN, ex.getMessage());
//    }
//
//    @ExceptionHandler(DuplicateItemSpecificationException.class)
//    public ResponseEntity<Map<String, Object>> handleDuplicateItemSpecification(DuplicateItemSpecificationException ex) {
//        return buildError(HttpStatus.CONFLICT, ex.getMessage());
//    }
//
//    @ExceptionHandler(InvalidItemOperationException.class)
//    public ResponseEntity<Map<String, Object>> handleInvalidItemOperation(InvalidItemOperationException ex) {
//        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
//    }
//
//    @ExceptionHandler(ItemImageNotFoundException.class)
//    public ResponseEntity<Map<String, Object>> handleItemImageNotFound(ItemImageNotFoundException ex) {
//        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
//    }
//
//    @ExceptionHandler(InvalidImageException.class)
//    public ResponseEntity<Map<String, Object>> handleInvalidImage(InvalidImageException ex) {
//        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
//    }
//
//    @ExceptionHandler(ImageStorageException.class)
//    public ResponseEntity<Map<String, Object>> handleImageStorage(ImageStorageException ex) {
//        return buildError(HttpStatus.BAD_GATEWAY, ex.getMessage());
//    }
//
//    // ---------- Auth ----------
//
//    @ExceptionHandler(AuthException.class)
//    public ResponseEntity<Map<String, Object>> handleAuthException(AuthException ex) {
//        log.warn("AuthException: {}", ex.getMessage());
//        return buildError(ex.getStatus(), ex.getMessage());
//    }
//
//    // ---------- Fallback ----------
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
//        log.error("Unexpected  {}", ex.getMessage());
//        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
//    }
//
//    @ExceptionHandler(UserProfileException.class)
//    public ResponseEntity<Map<String, Object>> handleUserProfileException(
//            UserProfileException ex) {
//        log.warn("UserProfileException: {}", ex.getMessage());
//        return buildError(ex.getStatus(), ex.getMessage());
//    }
//
//    @ExceptionHandler(co.istad.rentiq_api.features.kyc.exception.KycException.class)
//    public ResponseEntity<Map<String, Object>> handleKycException(
//            co.istad.rentiq_api.features.kyc.exception.KycException ex) {
//        log.warn("KycException: {}", ex.getMessage());
//        return buildError(ex.getStatus(), ex.getMessage());
//    }
//
//    // ---------- Helpers ----------
//
//    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String message) {
//        return buildErrorWithDetails(status, message, null);
//    }
//
//    private ResponseEntity<Map<String, Object>> buildErrorWithDetails(HttpStatus status, String message, Object errors) {
//        Map<String, Object> body = new HashMap<>();
//        body.put("timestamp", Instant.now().toString());
//        body.put("status", status.value());
//        body.put("error", status.getReasonPhrase());
//        body.put("message", message);
//        if (errors != null) {
//            body.put("errors", errors);
//        }
//        return ResponseEntity.status(status).body(body);
//    }
//}



package co.istad.rentiq_api.features;

import co.istad.rentiq_api.features.auth.exception.AuthException;
import co.istad.rentiq_api.features.category.exception.CategoryNotFoundException;
import co.istad.rentiq_api.features.category.exception.DuplicateCategoryException;
import co.istad.rentiq_api.features.item.exception.*;
import co.istad.rentiq_api.features.userProfile.exception.UserProfileException;
import co.istad.rentiq_api.features.kyc.exception.KycException;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    // =====================================================
    // Framework Exceptions
    // =====================================================

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException ex) {

        log.warn("ResponseStatusException: {}", ex.getReason());

        return buildError(
                HttpStatus.valueOf(ex.getStatusCode().value()),
                ex.getReason()
        );
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(
                    error.getField(),
                    error.getDefaultMessage()
            );
        }

        log.warn("Validation failed: {}", errors);

        return buildErrorWithDetails(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                errors
        );
    }


    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex) {

        log.warn("Constraint violation: {}", ex.getMessage());

        return buildError(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }


    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(
            NoResourceFoundException ex) {

        log.warn("Endpoint not found: {}", ex.getMessage());

        return buildError(
                HttpStatus.NOT_FOUND,
                "Endpoint not found"
        );
    }


    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDatabaseError(
            DataIntegrityViolationException ex) {

        log.error("Database error", ex);

        return buildError(
                HttpStatus.BAD_REQUEST,
                "Invalid data provided"
        );
    }


    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleFileSize(
            MaxUploadSizeExceededException ex) {

        log.warn("File upload too large");

        return buildError(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Uploaded file is too large"
        );
    }


    // =====================================================
    // Category Exceptions
    // =====================================================

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCategoryNotFound(
            CategoryNotFoundException ex) {

        return buildError(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
    }


    @ExceptionHandler(DuplicateCategoryException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateCategory(
            DuplicateCategoryException ex) {

        return buildError(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
    }



    // =====================================================
    // Item Exceptions
    // =====================================================

    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleItemNotFound(
            ItemNotFoundException ex) {

        return buildError(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
    }


    @ExceptionHandler(ItemAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleItemAccessDenied(
            ItemAccessDeniedException ex) {

        return buildError(
                HttpStatus.FORBIDDEN,
                ex.getMessage()
        );
    }


    @ExceptionHandler(DuplicateItemSpecificationException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateSpecification(
            DuplicateItemSpecificationException ex) {

        return buildError(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
    }


    @ExceptionHandler(InvalidItemOperationException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidItemOperation(
            InvalidItemOperationException ex) {

        return buildError(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }


    @ExceptionHandler(ItemImageNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleItemImageNotFound(
            ItemImageNotFoundException ex) {

        return buildError(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
    }


    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidImage(
            InvalidImageException ex) {

        return buildError(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }


    @ExceptionHandler(ImageStorageException.class)
    public ResponseEntity<Map<String, Object>> handleImageStorage(
            ImageStorageException ex) {

        log.error("Image storage error", ex);

        return buildError(
                HttpStatus.BAD_GATEWAY,
                ex.getMessage()
        );
    }



    // =====================================================
    // Auth
    // =====================================================

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, Object>> handleAuth(
            AuthException ex) {

        log.warn("Authentication error: {}", ex.getMessage());

        return buildError(
                ex.getStatus(),
                ex.getMessage()
        );
    }



    // =====================================================
    // User Profile
    // =====================================================

    @ExceptionHandler(UserProfileException.class)
    public ResponseEntity<Map<String, Object>> handleUserProfile(
            UserProfileException ex) {

        log.warn("User profile error: {}", ex.getMessage());

        return buildError(
                ex.getStatus(),
                ex.getMessage()
        );
    }



    // =====================================================
    // KYC
    // =====================================================

    @ExceptionHandler(KycException.class)
    public ResponseEntity<Map<String, Object>> handleKyc(
            KycException ex) {

        log.warn("KYC error: {}", ex.getMessage());

        return buildError(
                ex.getStatus(),
                ex.getMessage()
        );
    }



    // =====================================================
    // Fallback
    // =====================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(
            Exception ex) {

        log.error("Unexpected  {}", ex.getMessage());

        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
    }



    // =====================================================
    // Response Builder
    // =====================================================

    private ResponseEntity<Map<String, Object>> buildError(
            HttpStatus status,
            String message) {

        return buildErrorWithDetails(
                status,
                message,
                null
        );
    }


    private ResponseEntity<Map<String, Object>> buildErrorWithDetails(
            HttpStatus status,
            String message,
            Object errors) {

        Map<String, Object> body = new HashMap<>();

        body.put(
                "timestamp",
                Instant.now().toString()
        );

        body.put(
                "status",
                status.value()
        );

        body.put(
                "error",
                status.getReasonPhrase()
        );

        body.put(
                "message",
                message
        );

        if (errors != null) {
            body.put(
                    "errors",
                    errors
            );
        }

        return ResponseEntity
                .status(status)
                .body(body);
    }
}