package co.istad.rentiq_api.features.kyc.exception;


import org.springframework.http.HttpStatus;

public class KycNotFoundException extends KycException {
    public KycNotFoundException() {
        super(HttpStatus.NOT_FOUND, "KYC submission not found");
    }
}
