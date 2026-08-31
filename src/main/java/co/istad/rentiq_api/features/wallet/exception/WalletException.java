package co.istad.rentiq_api.features.wallet.exception;


import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
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

    public static WalletException invalidAmount() {
        return new WalletException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
    }

    public static WalletException insufficientBalance(BigDecimal balance, BigDecimal amount) {
        return new WalletException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Insufficient wallet balance: balance is %s but debit amount is %s".formatted(balance, amount));
    }

    public static WalletException notAVendor(String ownerId) {
        return new WalletException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Wallet owner " + ownerId + " does not have an approved vendor application");
    }

    public static WalletException currencyMismatch(String walletCurrency, String requestCurrency) {
        return new WalletException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Wallet currency is %s but top-up currency was %s — currency conversion is not supported"
                        .formatted(walletCurrency, requestCurrency));
    }
}
