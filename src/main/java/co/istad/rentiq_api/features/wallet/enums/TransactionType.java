package co.istad.rentiq_api.features.wallet.enums;

public enum TransactionType {
    WELCOME_BONUS,
    COMMISSION,
    // No longer produced: was credited on booking completion under an earlier escrow model.
    // Rentiq never collects/holds rental payment (it's P2P, renter pays vendor directly), so
    // nothing creates this transaction anymore. Kept only so historical rows keep resolving
    // and TransactionType.valueOf(...) call sites don't need a migration to drop it.
    BOOKING_EARNING,
    PENALTY,
    TOP_UP,
    REFUND,
    ADVERTISEMENT,
    PROMOTION,
    ADMIN_ADJUSTMENT
}
