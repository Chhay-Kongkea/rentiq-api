package co.istad.rentiq_api.features.wallet.service;


public interface WalletService {
    void grantWelcomeBonusIfEligible(String userId);
}