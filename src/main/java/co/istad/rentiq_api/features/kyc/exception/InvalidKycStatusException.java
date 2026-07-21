package co.istad.rentiq_api.features.kyc.exception;


import org.springframework.http.HttpStatus;

public class InvalidKycStatusException extends KycException {
    public InvalidKycStatusException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}