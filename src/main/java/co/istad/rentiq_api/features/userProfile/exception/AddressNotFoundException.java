package co.istad.rentiq_api.features.userProfile.exception;



import org.springframework.http.HttpStatus;

public class AddressNotFoundException extends UserProfileException {
    public AddressNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Address not found");
    }
}