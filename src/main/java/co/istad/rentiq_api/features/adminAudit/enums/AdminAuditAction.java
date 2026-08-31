package co.istad.rentiq_api.features.adminAudit.enums;

/**
 * Only actions with a corresponding, already-implemented admin mutation are listed here —
 * this enum is not a roadmap of future admin operations.
 */
public enum AdminAuditAction {
    USER_SUSPENDED,
    USER_BANNED,
    USER_REINSTATED,

    VENDOR_APPLICATION_APPROVED,
    VENDOR_APPLICATION_REJECTED,

    VENDOR_SUSPENDED,
    VENDOR_BANNED,
    VENDOR_REINSTATED,

    KYC_APPROVED,
    KYC_REJECTED,

    ITEM_APPROVED,
    ITEM_REJECTED,
    ITEM_REMOVED,
    ITEM_FEATURED,
    ITEM_UNFEATURED,

    BOOKING_STATUS_CHANGED,

    DISPUTE_RESOLVED,

    REPORT_STATUS_CHANGED,
    MODERATION_ACTION_CREATED,

    REVIEW_HIDDEN,
    REVIEW_RESTORED,

    WALLET_CREDITED,
    WALLET_DEBITED,

    // Admin directly funded a vendor's wallet after verifying an external (P2P) payment —
    // distinct from WALLET_CREDITED (a generic manual adjustment/correction) and from
    // TOPUP_CONFIRMED (confirming a vendor-submitted TopupRequest).
    WALLET_TOPPED_UP,

    TOPUP_CONFIRMED,

    COMMISSION_RATE_UPDATED,

    CATEGORY_CREATED,
    CATEGORY_UPDATED,
    CATEGORY_DELETED,
    CATEGORY_STATUS_CHANGED,

    ADVERTISEMENT_APPROVED,
    ADVERTISEMENT_REJECTED,
    ADVERTISEMENT_EXPIRED,

    PROMOTION_SUSPENDED,

    PLATFORM_SETTING_UPDATED
}
