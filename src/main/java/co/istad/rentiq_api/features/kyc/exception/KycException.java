package co.istad.rentiq_api.features.kyc.exception;


import org.springframework.http.HttpStatus;

public class KycException extends RuntimeException {
    private final HttpStatus status;

    public KycException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}