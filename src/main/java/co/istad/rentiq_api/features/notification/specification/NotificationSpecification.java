package co.istad.rentiq_api.features.notification.specification;

import co.istad.rentiq_api.features.notification.entity.Notification;
import co.istad.rentiq_api.features.notification.enums.NotificationReferenceType;
import co.istad.rentiq_api.features.notification.enums.NotificationType;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class NotificationSpecification {

    private NotificationSpecification() {
    }

    public static Specification<Notification> adminFilter(
            String userId,
            NotificationType type,
            Boolean read,
            NotificationReferenceType referenceType,
            UUID referenceId,
            OffsetDateTime createdFromInclusive,
            OffsetDateTime createdToExclusive
    ) {
        return Specification.allOf(
                userIdEquals(userId),
                typeEquals(type),
                readEquals(read),
                referenceTypeEquals(referenceType),
                referenceIdEquals(referenceId),
                createdAtFrom(createdFromInclusive),
                createdAtTo(createdToExclusive)
        );
    }

    private static Specification<Notification> userIdEquals(String userId) {
        return (root, query, cb) ->
                userId == null || userId.isBlank()
                        ? cb.conjunction()
                        : cb.equal(root.get("userId"), userId.trim());
    }

    private static Specification<Notification> typeEquals(NotificationType type) {
        return (root, query, cb) ->
                type == null ? cb.conjunction() : cb.equal(root.get("notificationType"), type);
    }

    private static Specification<Notification> readEquals(Boolean read) {
        return (root, query, cb) ->
                read == null ? cb.conjunction() : cb.equal(root.get("read"), read);
    }

    private static Specification<Notification> referenceTypeEquals(NotificationReferenceType referenceType) {
        return (root, query, cb) ->
                referenceType == null ? cb.conjunction() : cb.equal(root.get("referenceType"), referenceType);
    }

    private static Specification<Notification> referenceIdEquals(UUID referenceId) {
        return (root, query, cb) ->
                referenceId == null ? cb.conjunction() : cb.equal(root.get("referenceId"), referenceId);
    }

    private static Specification<Notification> createdAtFrom(OffsetDateTime fromInclusive) {
        return (root, query, cb) ->
                fromInclusive == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(root.get("createdAt"), fromInclusive);
    }

    private static Specification<Notification> createdAtTo(OffsetDateTime toExclusive) {
        return (root, query, cb) ->
                toExclusive == null
                        ? cb.conjunction()
                        : cb.lessThan(root.get("createdAt"), toExclusive);
    }
}
