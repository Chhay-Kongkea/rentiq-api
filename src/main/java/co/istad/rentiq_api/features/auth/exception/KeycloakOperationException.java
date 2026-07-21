package co.istad.rentiq_api.features.auth.exception;

import org.springframework.http.HttpStatus;

public class KeycloakOperationException extends AuthException {
    public KeycloakOperationException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public KeycloakOperationException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
        initCause(cause);
    }
}