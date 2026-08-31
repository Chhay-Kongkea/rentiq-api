package co.istad.rentiq_api.features.search.specification;

import co.istad.rentiq_api.features.item.entity.Item;
import co.istad.rentiq_api.features.item.enums.ItemApprovalStatus;
import co.istad.rentiq_api.features.item.enums.ItemStatus;
import co.istad.rentiq_api.features.promotion.entity.Promotion;
import co.istad.rentiq_api.features.promotion.enums.PromotionStatus;
import co.istad.rentiq_api.features.search.dto.request.ItemSearchFilter;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

public final class SearchItemSpecification {

    private SearchItemSpecification() {}

    /**
     * sortField must already be validated against an allow-list by the caller (see
     * SearchServiceImpl) — it's interpolated as a raw entity attribute path here.
     */
    public static Specification<Item> build(ItemSearchFilter filter, String sortField, Sort.Direction sortDirection) {
        return Specification
                .where(notDeleted())
                .and(approved())
                .and(active())
                .and(keywordContains(filter.keyword()))
                .and(minimumPrice(filter))
                .and(maximumPrice(filter))
                .and(categoryEquals(filter))
                .and(conditionEquals(filter))
                .and(availabilityEquals(filter))
                .and(featuredEquals(filter))
                .and(locationContains(filter))
                .and(minimumRating(filter))
                .and(promotedFirstThenRequestedOrder(sortField, sortDirection));
    }

    /**
     * Side-effecting: sets the query's ORDER BY directly — promoted items first (an EXISTS
     * subquery against effectively-active Promotions, evaluated DB-side, before pagination),
     * then the caller's requested secondary field/direction. This never touches the WHERE
     * predicates above, so it cannot bypass keyword/category/filter relevance — it only
     * reorders items that already satisfied every other criterion.
     *
     * Must be paired with an UNSORTED Pageable (see SearchServiceImpl): Spring Data applies
     * Pageable.getSort() to the query AFTER the Specification runs and would silently
     * overwrite this order otherwise.
     */
    private static Specification<Item> promotedFirstThenRequestedOrder(String sortField, Sort.Direction sortDirection) {
        return (root, query, cb) -> {
            boolean isRowQuery = query.getResultType() != Long.class && query.getResultType() != long.class;

            if (isRowQuery) {
                Subquery<UUID> activePromotion = query.subquery(UUID.class);
                Root<Promotion> promotionRoot = activePromotion.from(Promotion.class);
                OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

                activePromotion.select(promotionRoot.get("id"))
                        .where(
                                cb.equal(promotionRoot.get("itemId"), root.get("id")),
                                cb.equal(promotionRoot.get("status"), PromotionStatus.ACTIVE),
                                cb.lessThanOrEqualTo(promotionRoot.get("startAt"), now),
                                cb.greaterThan(promotionRoot.get("endAt"), now)
                        );

                Expression<Integer> promotedRank = cb.<Integer>selectCase()
                        .when(cb.exists(activePromotion), 0)
                        .otherwise(1);

                Order secondary = sortDirection == Sort.Direction.ASC
                        ? cb.asc(root.get(sortField))
                        : cb.desc(root.get(sortField));

                query.orderBy(cb.asc(promotedRank), secondary);
            }

            return cb.conjunction();
        };
    }

    private static Specification<Item> notDeleted() {
        return (root, query, cb) ->
                cb.isFalse(root.get("deleted"));
    }

    private static Specification<Item> approved() {
        return (root, query, cb) ->
                cb.equal(
                        root.get("approvalStatus"),
                        ItemApprovalStatus.APPROVED
                );
    }

    private static Specification<Item> active() {
        return (root, query, cb) ->
                cb.equal(
                        root.get("status"),
                        ItemStatus.ACTIVE
                );
    }

    private static Specification<Item> keywordContains(
            String keyword
    ) {
        return (root, query, cb) -> {
            if (keyword == null) {
                return cb.conjunction();
            }

            String pattern =
                    "%"
                            + keyword
                            .toLowerCase(Locale.ROOT)
                            + "%";

            return cb.or(
                    cb.like(
                            cb.lower(root.get("title")),
                            pattern
                    ),
                    cb.like(
                            cb.lower(root.get("description")),
                            pattern
                    )
            );
        };
    }

    private static Specification<Item> minimumPrice(ItemSearchFilter filter) {
        return (root, query, cb) ->
                filter.minPrice() == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(
                        root.get("pricePerDay"),
                        filter.minPrice()
                );
    }

    private static Specification<Item> maximumPrice(ItemSearchFilter filter) {
        return (root, query, cb) ->
                filter.maxPrice() == null
                        ? cb.conjunction()
                        : cb.lessThanOrEqualTo(
                        root.get("pricePerDay"),
                        filter.maxPrice()
                );
    }

    private static Specification<Item> categoryEquals(ItemSearchFilter filter) {
        return (root, query, cb) ->
                filter.categoryId() == null
                        ? cb.conjunction()
                        : cb.equal(
                        root.get("categoryId"),
                        filter.categoryId()
                );
    }

    private static Specification<Item> conditionEquals(ItemSearchFilter filter) {
        return (root, query, cb) ->
                filter.condition() == null
                        ? cb.conjunction()
                        : cb.equal(
                        root.get("condition"),
                        filter.condition()
                );
    }

    private static Specification<Item> availabilityEquals(ItemSearchFilter filter) {
        return (root, query, cb) -> {
            if (filter.available() == null) {
                return cb.isTrue(root.get("available"));
            }

            return cb.equal(
                    root.get("available"),
                    filter.available()
            );
        };
    }

    private static Specification<Item> featuredEquals(ItemSearchFilter filter) {
        return (root, query, cb) ->
                filter.featured() == null
                        ? cb.conjunction()
                        : cb.equal(
                        root.get("featured"),
                        filter.featured()
                );
    }

    private static Specification<Item> locationContains(ItemSearchFilter filter) {
        return (root, query, cb) -> {
            if (filter.location() == null) {
                return cb.conjunction();
            }

            String pattern =
                    "%"
                            + filter.location()
                            .toLowerCase(Locale.ROOT)
                            + "%";

            return cb.like(
                    cb.lower(root.get("locationText")),
                    pattern
            );
        };
    }

    private static Specification<Item> minimumRating(ItemSearchFilter filter) {
        return (root, query, cb) ->
                filter.minimumRating() == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(
                        root.get("averageRating"),
                        filter.minimumRating()
                );
    }
}