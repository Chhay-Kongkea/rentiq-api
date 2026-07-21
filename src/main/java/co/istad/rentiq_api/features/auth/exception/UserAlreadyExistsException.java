package co.istad.rentiq_api.features.auth.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends AuthException {
    public UserAlreadyExistsException(String username) {
        super(HttpStatus.CONFLICT, "User already exists: " + username);
    }
}