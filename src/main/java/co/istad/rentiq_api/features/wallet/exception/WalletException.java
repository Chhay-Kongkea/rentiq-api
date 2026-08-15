package co.istad.rentiq_api.features.wallet.exception;


import org.springframework.http.HttpStatus;

import java.util.UUID;

public class WalletException extends RuntimeException {

    private final HttpStatus status;

    public WalletException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static WalletException notFoundForOwner(String ownerId) {
        return new WalletException(HttpStatus.NOT_FOUND, "Wallet not found for owner: " + ownerId);
    }

    public static WalletException notFoundById(UUID walletId) {
        return new WalletException(HttpStatus.NOT_FOUND, "Wallet not found with ID: " + walletId);
    }

    public static WalletException transactionNotFound(UUID transactionId) {
        return new WalletException(HttpStatus.NOT_FOUND, "Wallet transaction not found with ID: " + transactionId);
    }

    public static WalletException topupNotFound(UUID topupRequestId) {
        return new WalletException(HttpStatus.NOT_FOUND, "Top-up request not found with ID: " + topupRequestId);
    }

    public static WalletException topupNotFoundByReference(String bankReference) {
        return new WalletException(HttpStatus.NOT_FOUND, "Top-up request not found for bank reference: " + bankReference);
    }

    public static WalletException invalidSignature() {
        return new WalletException(HttpStatus.UNAUTHORIZED, "Webhook signature is missing or invalid");
    }

    public static WalletException amountMismatch() {
        return new WalletException(HttpStatus.UNPROCESSABLE_ENTITY, "Webhook amount does not match the recorded top-up amount");
    }

    public static WalletException invalidTopupState(String currentStatus) {
        return new WalletException(HttpStatus.CONFLICT, "Top-up request is not pending, current status: " + currentStatus);
    }

    public static WalletException invalidAmount() {
        return new WalletException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
    }
}
