package co.istad.rentiq_api.features.adminAudit;

import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditPersistedAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditPersistedTargetType;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import org.springframework.stereotype.Component;

@Component
public class AdminAuditPersistenceMapper {

    public AdminAuditPersistedAction resolveAction(AdminAuditAction semanticAction) {
        try {
            return AdminAuditPersistedAction.valueOf(semanticAction.name());
        } catch (IllegalArgumentException ignored) {
            return switch (semanticAction) {
                case WALLET_TOPPED_UP -> AdminAuditPersistedAction.WALLET_CREDITED;
                case CATEGORY_CREATED, CATEGORY_UPDATED, CATEGORY_DELETED,
                     CATEGORY_STATUS_CHANGED, ADVERTISEMENT_APPROVED,
                     ADVERTISEMENT_REJECTED, ADVERTISEMENT_EXPIRED,
                     PROMOTION_SUSPENDED, PLATFORM_SETTING_UPDATED ->
                        AdminAuditPersistedAction.MODERATION_ACTION_CREATED;
                default -> throw new IllegalStateException(
                        "No persisted Admin Audit action mapping for " + semanticAction
                );
            };
        }
    }

    public AdminAuditPersistedTargetType resolveTargetType(AdminAuditTargetType semanticTargetType) {
        try {
            return AdminAuditPersistedTargetType.valueOf(semanticTargetType.name());
        } catch (IllegalArgumentException ignored) {
            return switch (semanticTargetType) {
                case ADVERTISEMENT, PROMOTION -> AdminAuditPersistedTargetType.ITEM;
                case PLATFORM_SETTING -> AdminAuditPersistedTargetType.CATEGORY;
                default -> throw new IllegalStateException(
                        "No persisted Admin Audit target mapping for " + semanticTargetType
                );
            };
        }
    }
}
