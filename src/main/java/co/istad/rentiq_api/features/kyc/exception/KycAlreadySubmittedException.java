package co.istad.rentiq_api.features.kyc.exception;


import org.springframework.http.HttpStatus;

public class KycAlreadySubmittedException extends KycException {
    public KycAlreadySubmittedException() {
        super(HttpStatus.CONFLICT, "KYC has already been submitted. Use resubmit instead.");
    }
}