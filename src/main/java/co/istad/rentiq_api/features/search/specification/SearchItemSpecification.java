package co.istad.rentiq_api.features.search.specification;

import co.istad.rentiq_api.features.item.entity.Item;
import co.istad.rentiq_api.features.item.enums.ItemApprovalStatus;
import co.istad.rentiq_api.features.item.enums.ItemStatus;
import co.istad.rentiq_api.features.search.dto.request.ItemSearchFilter;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class SearchItemSpecification {

    private SearchItemSpecification() {}

    public static Specification<Item> build(ItemSearchFilter filter) {
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
                .and(minimumRating(filter));
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