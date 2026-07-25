package co.istad.rentiq_api.features.userProfile.exception;


import org.springframework.http.HttpStatus;

public class InvalidAvatarException extends UserProfileException {
    public InvalidAvatarException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}