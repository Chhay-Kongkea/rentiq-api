package co.istad.rentiq_api.features.advertisement.specification;

import co.istad.rentiq_api.features.advertisement.entity.Advertisement;
import co.istad.rentiq_api.features.advertisement.enums.AdvertisementStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;

public final class AdvertisementSpecification {

    private AdvertisementSpecification() {
    }

    public static Specification<Advertisement> adminFilter(
            AdvertisementStatus status,
            String vendorId,
            OffsetDateTime fromInclusive,
            OffsetDateTime toExclusive
    ) {
        return Specification.allOf(
                statusEquals(status),
                vendorIdEquals(vendorId),
                createdAtFrom(fromInclusive),
                createdAtTo(toExclusive)
        );
    }

    private static Specification<Advertisement> statusEquals(AdvertisementStatus status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    private static Specification<Advertisement> vendorIdEquals(String vendorId) {
        return (root, query, cb) ->
                vendorId == null || vendorId.isBlank()
                        ? cb.conjunction()
                        : cb.equal(root.get("vendorId"), vendorId.trim());
    }

    private static Specification<Advertisement> createdAtFrom(OffsetDateTime fromInclusive) {
        return (root, query, cb) ->
                fromInclusive == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(root.get("createdAt"), fromInclusive);
    }

    private static Specification<Advertisement> createdAtTo(OffsetDateTime toExclusive) {
        return (root, query, cb) ->
                toExclusive == null
                        ? cb.conjunction()
                        : cb.lessThan(root.get("createdAt"), toExclusive);
    }
}
