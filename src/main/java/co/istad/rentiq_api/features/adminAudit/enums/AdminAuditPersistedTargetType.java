package co.istad.rentiq_api.features.adminAudit.enums;

/** Exact target values accepted by the legacy PostgreSQL CHECK constraint. */
public enum AdminAuditPersistedTargetType {
    USER,
    VENDOR,
    VENDOR_APPLICATION,
    KYC,
    ITEM,
    BOOKING,
    DISPUTE,
    REPORT,
    REVIEW,
    WALLET,
    TOPUP,
    CATEGORY
}
