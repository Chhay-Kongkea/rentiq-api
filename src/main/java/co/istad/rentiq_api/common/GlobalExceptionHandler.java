package co.istad.rentiq_api.common;

import co.istad.rentiq_api.common.dto.ApiErrorResponse;
import co.istad.rentiq_api.common.exception.DuplicateException;
import co.istad.rentiq_api.common.exception.ForbiddenException;
import co.istad.rentiq_api.common.exception.InvalidOperationException;
import co.istad.rentiq_api.common.exception.InvalidStateException;
import co.istad.rentiq_api.common.exception.NotFoundException;
import co.istad.rentiq_api.common.exception.StorageException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /*
     * 404 Not Found
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleNotFoundException(NotFoundException exception, HttpServletRequest request) {
        return ApiErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                "RESOURCE_NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI(),
                exception.getDetails()
        );
    }

    /*
     * 403 Forbidden
     */
    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiErrorResponse handleForbiddenException(ForbiddenException exception, HttpServletRequest request) {
        return ApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "FORBIDDEN",
                exception.getMessage(),
                request.getRequestURI(),
                exception.getDetails()
        );
    }

    /*
     * 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiErrorResponse handleAccessDeniedException(AccessDeniedException exception, HttpServletRequest request) {
        return ApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "ACCESS_DENIED",
                "You do not have permission to perform this action",
                request.getRequestURI(),
                Map.of()
        );
    }

    /*
     * 409 Conflict
     */
    @ExceptionHandler(DuplicateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleDuplicateException(DuplicateException exception, HttpServletRequest request) {
        return ApiErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                "DUPLICATE_RESOURCE",
                exception.getMessage(),
                request.getRequestURI(),
                exception.getDetails()
        );
    }

    /*
     * 409 Conflict
     */
    @ExceptionHandler(InvalidStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleInvalidStateException(InvalidStateException exception, HttpServletRequest request) {
        return ApiErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                "INVALID_RESOURCE_STATE",
                exception.getMessage(),
                request.getRequestURI(),
                exception.getDetails()
        );
    }

    /*
     * 400 Bad Request
     */
    @ExceptionHandler(InvalidOperationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleInvalidOperationException(InvalidOperationException exception, HttpServletRequest request) {
        return ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "INVALID_OPERATION",
                exception.getMessage(),
                request.getRequestURI(),
                exception.getDetails()
        );
    }

    /*
     * 500 Internal Server Error
     */
    @ExceptionHandler(StorageException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleStorageException(StorageException exception, HttpServletRequest request) {
        return ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR
                        .getReasonPhrase(),
                "STORAGE_ERROR",
                exception.getMessage(),
                request.getRequestURI(),
                exception.getDetails()
        );
    }

    /*
     * 400 - request body errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleMethodArgumentNotValid(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> validationErrors = new LinkedHashMap<>();

        exception.getBindingResult()
                .getAllErrors()
                .forEach(error -> {
                    String fieldName;

                    if (error instanceof FieldError fieldError) {
                        fieldName = fieldError.getField();
                    } else {
                        fieldName = error.getObjectName();
                    }

                    validationErrors.put(
                            fieldName,
                            error.getDefaultMessage()
                    );
                });

        return ApiErrorResponse.validation(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "VALIDATION_FAILED",
                "Request validation failed",
                request.getRequestURI(),
                validationErrors
        );
    }

    /*
     * 400 - validation errors
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
        Map<String, String> validationErrors =
                new LinkedHashMap<>();

        for (ConstraintViolation<?> violation
                : exception.getConstraintViolations()) {

            validationErrors.put(
                    violation
                            .getPropertyPath()
                            .toString(),
                    violation.getMessage()
            );
        }

        return ApiErrorResponse.validation(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "VALIDATION_FAILED",
                "Request validation failed",
                request.getRequestURI(),
                validationErrors
        );
    }

    /*
     * 400 - Invalid field
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        String expectedType =
                exception.getRequiredType() == null
                        ? "valid value"
                        : exception
                          .getRequiredType()
                          .getSimpleName();

        String message =
                "Invalid value '%s' for parameter '%s'. Expected %s"
                        .formatted(
                                exception.getValue(),
                                exception.getName(),
                                expectedType
                        );

        return ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "INVALID_PARAMETER",
                message,
                request.getRequestURI(),
                Map.of(
                        "parameter",
                        exception.getName(),
                        "providedValue",
                        String.valueOf(
                                exception.getValue()
                        ),
                        "expectedType",
                        expectedType
                )
        );
    }

    /*
     * 400 - Missing query
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleMissingParameter(MissingServletRequestParameterException exception, HttpServletRequest request) {
        return ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "MISSING_PARAMETER",
                "Required parameter '%s' is missing"
                        .formatted(
                                exception.getParameterName()
                        ),
                request.getRequestURI(),
                Map.of(
                        "parameter",
                        exception.getParameterName()
                )
        );
    }

    /*
     * 400 - Invalid
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleUnreadableRequest(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "INVALID_REQUEST_BODY",
                "The request body is invalid or malformed",
                request.getRequestURI(),
                Map.of()
        );
    }

    /*
     * 400 - invalid something
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "INVALID_ARGUMENT",
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    /*
     * 409 - unique
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleDataIntegrityViolation(DataIntegrityViolationException exception, HttpServletRequest request) {
        return ApiErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                "DATA_INTEGRITY_VIOLATION",
                "The operation violates a database constraint",
                request.getRequestURI(),
                Map.of()
        );
    }

    /*
     * 500 errors
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleUnexpectedException(Exception exception, HttpServletRequest request) {
        return ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR
                        .getReasonPhrase(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected internal server error occurred",
                request.getRequestURI(),
                Map.of()
        );
    }
}