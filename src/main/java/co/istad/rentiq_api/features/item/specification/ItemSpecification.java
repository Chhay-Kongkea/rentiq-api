package co.istad.rentiq_api.features.item.specification;

import co.istad.rentiq_api.features.item.dto.request.ItemFilter;
import co.istad.rentiq_api.features.item.entity.Item;
import co.istad.rentiq_api.features.item.enums.ItemApprovalStatus;
import co.istad.rentiq_api.features.item.enums.ItemStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ItemSpecification {

    private ItemSpecification() {
    }

    public static Specification<Item> publicFilter(
            ItemFilter filter
    ) {
        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(
                    criteriaBuilder.isFalse(
                            root.get("deleted")
                    )
            );

            predicates.add(
                    criteriaBuilder.equal(
                            root.get("approvalStatus"),
                            ItemApprovalStatus.APPROVED
                    )
            );

            predicates.add(
                    criteriaBuilder.equal(
                            root.get("status"),
                            ItemStatus.ACTIVE
                    )
            );

            if (filter == null) {
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            }

            if (filter.keyword() != null && !filter.keyword().isBlank()) {
                String keyword = "%"
                        + filter.keyword()
                        .trim()
                        .toLowerCase(Locale.ROOT)
                        + "%";

                Predicate titlePredicate = criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("title")
                                ),
                                keyword
                        );

                Predicate descriptionPredicate = criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("description")
                                ),
                                keyword
                        );

                predicates.add(
                        criteriaBuilder.or(
                                titlePredicate,
                                descriptionPredicate
                        )
                );
            }

            if (filter.minPrice() != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("pricePerDay"),
                                filter.minPrice()
                        )
                );
            }

            if (filter.maxPrice() != null) {
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("pricePerDay"),
                                filter.maxPrice()
                        )
                );
            }

            if (filter.categoryId() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("categoryId"),
                                filter.categoryId()
                        )
                );
            }

            if (filter.condition() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("condition"),
                                filter.condition()
                        )
                );
            }

            if (filter.available() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("available"),
                                filter.available()
                        )
                );
            }

            if (filter.featured() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("featured"),
                                filter.featured()
                        )
                );
            }

            if (filter.location() != null && !filter.location().isBlank()) {
                String location = "%"
                        + filter.location()
                        .trim()
                        .toLowerCase(Locale.ROOT)
                        + "%";

                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("locationText")
                                ),
                                location
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }

    public static Specification<Item> publicVendorItems(
            String ownerId
    ) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.and(
                        criteriaBuilder.equal(
                                root.get("ownerId"),
                                ownerId
                        ),
                        criteriaBuilder.isFalse(
                                root.get("deleted")
                        ),
                        criteriaBuilder.equal(
                                root.get("approvalStatus"),
                                ItemApprovalStatus.APPROVED
                        ),
                        criteriaBuilder.equal(
                                root.get("status"),
                                ItemStatus.ACTIVE
                        )
                );
    }

    public static Specification<Item> vendorOwnedItems(
            String ownerId
    ) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.and(
                        criteriaBuilder.equal(
                                root.get("ownerId"),
                                ownerId
                        ),
                        criteriaBuilder.isFalse(
                                root.get("deleted")
                        )
                );
    }
}