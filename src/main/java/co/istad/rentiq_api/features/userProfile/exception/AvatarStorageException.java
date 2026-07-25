package co.istad.rentiq_api.features.userProfile.exception;


import org.springframework.http.HttpStatus;

public class AvatarStorageException extends UserProfileException {
    public AvatarStorageException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, message);
        initCause(cause);
    }
}