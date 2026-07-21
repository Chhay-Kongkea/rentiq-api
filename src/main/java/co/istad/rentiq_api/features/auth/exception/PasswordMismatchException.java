package co.istad.rentiq_api.features.auth.exception;

import org.springframework.http.HttpStatus;

public class PasswordMismatchException extends AuthException {
    public PasswordMismatchException() {
        super(HttpStatus.BAD_REQUEST, "Password and confirm password do not match");
    }
}