package co.istad.rentiq_api.features.item.specification;

import co.istad.rentiq_api.features.item.dto.request.AdminItemFilter;
import co.istad.rentiq_api.features.item.entity.Item;
import co.istad.rentiq_api.features.item.enums.ItemApprovalStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItemSpecificationTest {

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void adminModerationFilter_appliesPendingStatusWithoutPublicApprovedConstraint() {
        Root<Item> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Path deleted = mock(Path.class);
        Path approvalStatus = mock(Path.class);
        Predicate notDeleted = mock(Predicate.class);
        Predicate pending = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);

        when(root.get("deleted")).thenReturn(deleted);
        when(root.get("approvalStatus")).thenReturn(approvalStatus);
        when(builder.isFalse(deleted)).thenReturn(notDeleted);
        when(builder.equal(approvalStatus, ItemApprovalStatus.PENDING)).thenReturn(pending);
        when(builder.and(notDeleted, pending)).thenReturn(combined);

        ItemSpecification.adminModerationFilter(
                new AdminItemFilter(null, ItemApprovalStatus.PENDING)
        ).toPredicate(root, query, builder);

        verify(builder).equal(approvalStatus, ItemApprovalStatus.PENDING);
        verify(builder).and(any(Predicate[].class));
    }
}
