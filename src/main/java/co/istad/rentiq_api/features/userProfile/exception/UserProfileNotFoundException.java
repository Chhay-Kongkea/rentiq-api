package co.istad.rentiq_api.features.userProfile.exception;


import org.springframework.http.HttpStatus;

public class UserProfileNotFoundException extends UserProfileException {
    public UserProfileNotFoundException(String userId) {
        super(HttpStatus.NOT_FOUND, "User not found: " + userId);
    }
}