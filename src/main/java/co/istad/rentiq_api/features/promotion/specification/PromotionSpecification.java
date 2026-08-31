package co.istad.rentiq_api.features.promotion.specification;

import co.istad.rentiq_api.features.promotion.entity.Promotion;
import co.istad.rentiq_api.features.promotion.enums.PromotionPackage;
import co.istad.rentiq_api.features.promotion.enums.PromotionStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class PromotionSpecification {

    private PromotionSpecification() {
    }

    public static Specification<Promotion> adminFilter(
            PromotionStatus status,
            String vendorId,
            UUID itemId,
            PromotionPackage packageType,
            OffsetDateTime createdFromInclusive,
            OffsetDateTime createdToExclusive,
            OffsetDateTime now
    ) {
        return Specification.allOf(
                effectiveStatusEquals(status, now),
                vendorIdEquals(vendorId),
                itemIdEquals(itemId),
                packageTypeEquals(packageType),
                createdAtFrom(createdFromInclusive),
                createdAtTo(createdToExclusive)
        );
    }

    /**
     * Mirrors the "effective status" rule used everywhere else in this feature: an ACTIVE row
     * whose window has passed is treated as EXPIRED, and a request for EXPIRED must also match
     * those lapsed-but-still-ACTIVE rows — never rely on a scheduler to have flipped them.
     */
    private static Specification<Promotion> effectiveStatusEquals(PromotionStatus status, OffsetDateTime now) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return switch (status) {
                case ACTIVE -> cb.and(
                        cb.equal(root.get("status"), PromotionStatus.ACTIVE),
                        cb.lessThanOrEqualTo(root.get("startAt"), now),
                        cb.greaterThan(root.get("endAt"), now));
                case EXPIRED -> cb.or(
                        cb.equal(root.get("status"), PromotionStatus.EXPIRED),
                        cb.and(cb.equal(root.get("status"), PromotionStatus.ACTIVE),
                                cb.lessThanOrEqualTo(root.get("endAt"), now)));
                case CANCELLED, SUSPENDED -> cb.equal(root.get("status"), status);
            };
        };
    }

    private static Specification<Promotion> vendorIdEquals(String vendorId) {
        return (root, query, cb) ->
                vendorId == null || vendorId.isBlank()
                        ? cb.conjunction()
                        : cb.equal(root.get("vendorId"), vendorId.trim());
    }

    private static Specification<Promotion> itemIdEquals(UUID itemId) {
        return (root, query, cb) ->
                itemId == null ? cb.conjunction() : cb.equal(root.get("itemId"), itemId);
    }

    private static Specification<Promotion> packageTypeEquals(PromotionPackage packageType) {
        return (root, query, cb) ->
                packageType == null ? cb.conjunction() : cb.equal(root.get("packageType"), packageType);
    }

    private static Specification<Promotion> createdAtFrom(OffsetDateTime fromInclusive) {
        return (root, query, cb) ->
                fromInclusive == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(root.get("createdAt"), fromInclusive);
    }

    private static Specification<Promotion> createdAtTo(OffsetDateTime toExclusive) {
        return (root, query, cb) ->
                toExclusive == null
                        ? cb.conjunction()
                        : cb.lessThan(root.get("createdAt"), toExclusive);
    }
}
