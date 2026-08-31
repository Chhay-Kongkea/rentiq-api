package co.istad.rentiq_api.features.notification.enums;

public enum NotificationType {
    BOOKING,
    PAYMENT,
    ITEM_REQUEST,
    OFFER,
    ITEM,
    MARKETING,
    SYSTEM,

    // Added for business-event integration. Kept at the same coarse, domain-level granularity
    // as the existing values above (one type per domain, not one per state transition) —
    // ITEM approve/reject and wallet top-up deliberately reuse ITEM/PAYMENT above instead of
    // adding narrower variants.
    VENDOR_APPLICATION,
    KYC,
    ADVERTISEMENT,
    PROMOTION,
    DISPUTE,
    REPORT,
    REVIEW
}