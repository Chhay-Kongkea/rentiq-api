package co.istad.rentiq_api.features.item.service.impl;

import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
import co.istad.rentiq_api.features.notification.enums.NotificationReferenceType;
import co.istad.rentiq_api.features.notification.enums.NotificationType;
import co.istad.rentiq_api.features.notification.service.NotificationService;
import co.istad.rentiq_api.features.item.entity.Item;
import co.istad.rentiq_api.features.item.dto.request.AdminItemFilter;
import co.istad.rentiq_api.features.item.dto.respone.AdminItemResponse;
import co.istad.rentiq_api.features.item.dto.respone.ItemResponse;
import co.istad.rentiq_api.features.item.enums.ItemApprovalStatus;
import co.istad.rentiq_api.features.item.exception.InvalidItemOperationException;
import co.istad.rentiq_api.features.item.mapper.ItemMapper;
import co.istad.rentiq_api.features.item.repository.ItemRepository;
import co.istad.rentiq_api.features.item.service.ItemSpecificationValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock private ItemRepository itemRepository;
    @Mock private ItemMapper itemMapper;
    @Mock private AdminAuditService adminAuditService;
    @Mock private NotificationService notificationService;
    @Mock private ItemSpecificationValidator itemSpecificationValidator;

    private ItemServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ItemServiceImpl(
                itemRepository, itemMapper, adminAuditService, notificationService,
                itemSpecificationValidator
        );
        lenient().when(itemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Item pendingItem(UUID id) {
        return Item.builder().id(id).ownerId("owner-1").approvalStatus(ItemApprovalStatus.PENDING).build();
    }

    @Test
    void adminApproveItem_approvesPendingItem_andRecordsAudit() {
        UUID itemId = UUID.randomUUID();
        when(itemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.of(pendingItem(itemId)));

        service.adminApproveItem(itemId, "admin-1");

        verify(adminAuditService).record(
                AdminAuditAction.ITEM_APPROVED,
                AdminAuditTargetType.ITEM,
                itemId.toString(),
                Map.of("approvalStatus", "PENDING"),
                Map.of("approvalStatus", "APPROVED"),
                null);
        verify(notificationService).notifyUser(
                eq("owner-1"), eq(NotificationType.ITEM), any(), any(), eq(NotificationReferenceType.ITEM), eq(itemId));
    }

    @Test
    void adminApproveItem_rejectsAlreadyApprovedItem_andDoesNotRecordAudit() {
        UUID itemId = UUID.randomUUID();
        Item approved = Item.builder().id(itemId).approvalStatus(ItemApprovalStatus.APPROVED).build();
        when(itemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> service.adminApproveItem(itemId, "admin-1"))
                .isInstanceOf(InvalidItemOperationException.class);

        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any());
    }

    @Test
    void adminRejectItem_rejectsPendingItem_andRecordsAuditWithReason() {
        UUID itemId = UUID.randomUUID();
        when(itemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.of(pendingItem(itemId)));

        service.adminRejectItem(itemId, "Misleading photos", "admin-1");

        verify(adminAuditService).record(
                AdminAuditAction.ITEM_REJECTED,
                AdminAuditTargetType.ITEM,
                itemId.toString(),
                Map.of("approvalStatus", "PENDING"),
                Map.of("approvalStatus", "REJECTED"),
                "Misleading photos");
        verify(notificationService).notifyUser(
                eq("owner-1"), eq(NotificationType.ITEM), any(), any(), eq(NotificationReferenceType.ITEM), eq(itemId));
    }

    @Test
    void adminSetFeatured_true_recordsItemFeaturedAudit() {
        UUID itemId = UUID.randomUUID();
        Item item = Item.builder().id(itemId).approvalStatus(ItemApprovalStatus.APPROVED).featured(false).build();
        when(itemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.of(item));

        service.adminSetFeatured(itemId, true, null);

        verify(adminAuditService).record(
                org.mockito.ArgumentMatchers.eq(AdminAuditAction.ITEM_FEATURED),
                org.mockito.ArgumentMatchers.eq(AdminAuditTargetType.ITEM),
                org.mockito.ArgumentMatchers.eq(itemId.toString()),
                any(), any(), org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    void getAdminItems_returnsPendingItemsWithoutPublicVisibilityFilter() {
        UUID itemId = UUID.randomUUID();
        Item pending = pendingItem(itemId);
        ItemResponse itemResponse = org.mockito.Mockito.mock(ItemResponse.class);
        AdminItemResponse adminResponse = new AdminItemResponse(itemResponse, null, null, null);
        when(itemRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<Item>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(pending)));
        when(itemMapper.toAdminResponse(pending)).thenReturn(adminResponse);

        var result = service.getAdminItems(
                new AdminItemFilter(null, ItemApprovalStatus.PENDING),
                0, 20, "createdAt", "desc"
        );

        assertThat(result.content()).containsExactly(adminResponse);
        verify(itemRepository).findAll(
                org.mockito.ArgumentMatchers.<Specification<Item>>any(),
                any(Pageable.class)
        );
    }

    @Test
    void getAdminItemById_returnsPendingModerationDetail() {
        UUID itemId = UUID.randomUUID();
        Item pending = pendingItem(itemId);
        AdminItemResponse response = new AdminItemResponse(
                org.mockito.Mockito.mock(ItemResponse.class), null, null, null
        );
        when(itemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.of(pending));
        when(itemMapper.toAdminResponse(pending)).thenReturn(response);

        assertThat(service.getAdminItemById(itemId)).isSameAs(response);
    }
}
