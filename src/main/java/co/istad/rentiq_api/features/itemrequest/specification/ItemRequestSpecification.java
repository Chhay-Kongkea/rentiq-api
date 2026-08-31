package co.istad.rentiq_api.features.itemrequest.specification;

import co.istad.rentiq_api.features.itemrequest.dto.request.ItemRequestFilter;
import co.istad.rentiq_api.features.itemrequest.entity.ItemRequest;
import co.istad.rentiq_api.features.itemrequest.enums.ItemRequestStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.Locale;

public final class ItemRequestSpecification {

    private ItemRequestSpecification() {
    }

    public static Specification<ItemRequest> publicFilter(ItemRequestFilter filter) {
        return Specification
                .where(statusEquals(
                        filter.status() == null
                                ? ItemRequestStatus.OPEN
                                : filter.status()
                ))
                .and(notExpired())
                .and(keywordContains(filter.keyword()))
                .and(categoryEquals(filter.categoryId()))
                .and(budgetMinimum(filter))
                .and(budgetMaximum(filter));
    }

    private static Specification<ItemRequest> statusEquals(ItemRequestStatus status) {
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }

    private static Specification<ItemRequest> notExpired() {
        return (root, query, cb) ->
                cb.or(
                        cb.isNull(root.get("expiresAt")),
                        cb.greaterThan(
                                root.get("expiresAt"),
                                OffsetDateTime.now()
                        )
                );
    }

    private static Specification<ItemRequest> keywordContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null) {
                return cb.conjunction();
            }

            String pattern = "%"
                    + keyword.toLowerCase(Locale.ROOT)
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

    private static Specification<ItemRequest> categoryEquals(java.util.UUID categoryId) {
        return (root, query, cb) ->
                categoryId == null
                        ? cb.conjunction()
                        : cb.equal(
                        root.get("categoryId"),
                        categoryId
                );
    }

    private static Specification<ItemRequest> budgetMinimum(ItemRequestFilter filter) {
        return (root, query, cb) ->
                filter.budgetMin() == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(
                        root.get("budgetMax"),
                        filter.budgetMin()
                );
    }

    private static Specification<ItemRequest> budgetMaximum(ItemRequestFilter filter) {
        return (root, query, cb) ->
                filter.budgetMax() == null
                        ? cb.conjunction()
                        : cb.lessThanOrEqualTo(
                        root.get("budgetMin"),
                        filter.budgetMax()
                );
    }
}
