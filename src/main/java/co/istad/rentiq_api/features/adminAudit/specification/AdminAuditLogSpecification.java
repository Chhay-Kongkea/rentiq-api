package co.istad.rentiq_api.features.adminAudit.specification;

import co.istad.rentiq_api.features.adminAudit.entity.AdminAuditLog;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditPersistedAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditPersistedTargetType;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;

public final class AdminAuditLogSpecification {

    private AdminAuditLogSpecification() {
    }

    public static Specification<AdminAuditLog> filter(
            String adminId,
            AdminAuditPersistedAction action,
            AdminAuditPersistedTargetType targetType,
            String targetId,
            OffsetDateTime fromInclusive,
            OffsetDateTime toExclusive
    ) {
        return Specification.allOf(
                adminIdEquals(adminId),
                actionEquals(action),
                targetTypeEquals(targetType),
                targetIdEquals(targetId),
                createdAtFrom(fromInclusive),
                createdAtTo(toExclusive)
        );
    }

    private static Specification<AdminAuditLog> adminIdEquals(String adminId) {
        return (root, query, cb) ->
                adminId == null || adminId.isBlank()
                        ? cb.conjunction()
                        : cb.equal(root.get("adminId"), adminId.trim());
    }

    private static Specification<AdminAuditLog> actionEquals(AdminAuditPersistedAction action) {
        return (root, query, cb) ->
                action == null ? cb.conjunction() : cb.equal(root.get("action"), action);
    }

    private static Specification<AdminAuditLog> targetTypeEquals(AdminAuditPersistedTargetType targetType) {
        return (root, query, cb) ->
                targetType == null ? cb.conjunction() : cb.equal(root.get("targetType"), targetType);
    }

    private static Specification<AdminAuditLog> targetIdEquals(String targetId) {
        return (root, query, cb) ->
                targetId == null || targetId.isBlank()
                        ? cb.conjunction()
                        : cb.equal(root.get("targetId"), targetId.trim());
    }

    private static Specification<AdminAuditLog> createdAtFrom(OffsetDateTime fromInclusive) {
        return (root, query, cb) ->
                fromInclusive == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(root.get("createdAt"), fromInclusive);
    }

    private static Specification<AdminAuditLog> createdAtTo(OffsetDateTime toExclusive) {
        return (root, query, cb) ->
                toExclusive == null
                        ? cb.conjunction()
                        : cb.lessThan(root.get("createdAt"), toExclusive);
    }
}
