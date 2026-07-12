package co.istad.rentiq_api.features.item.service.impl;

import co.istad.rentiq_api.exception.ItemAccessDeniedException;
import co.istad.rentiq_api.exception.ItemNotFoundException;
import co.istad.rentiq_api.features.item.dto.respone.PageResponse;
import co.istad.rentiq_api.features.item.dto.request.CreateItemRequest;
import co.istad.rentiq_api.features.item.dto.request.ItemFilter;
import co.istad.rentiq_api.features.item.dto.request.UpdateItemRequest;
import co.istad.rentiq_api.features.item.dto.respone.ItemResponse;
import co.istad.rentiq_api.features.item.entity.Item;
import co.istad.rentiq_api.features.item.enums.ItemApprovalStatus;
import co.istad.rentiq_api.features.item.enums.ItemStatus;
import co.istad.rentiq_api.features.item.mapper.ItemMapper;
import co.istad.rentiq_api.features.item.repository.ItemRepository;
import co.istad.rentiq_api.features.item.service.ItemService;
import co.istad.rentiq_api.features.item.specification.ItemSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private static final int MAXIMUM_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "title",
            "pricePerDay",
            "averageRating",
            "totalBookings",
            "favoriteCount",
            "viewCount",
            "createdAt",
            "updatedAt"
    );

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    @Override
    @Transactional
    public ItemResponse createItem(CreateItemRequest request, String authenticatedUserId) {

        Item item = itemMapper.toEntity(request, authenticatedUserId);
        Item savedItem = itemRepository.save(item);

        return itemMapper.toResponse(savedItem);
    }

    @Override
    public ItemResponse getPublicItemById(UUID itemId) {
        Item item = itemRepository
                .findByIdAndDeletedFalseAndApprovalStatusAndStatus(
                        itemId,
                        ItemApprovalStatus.APPROVED,
                        ItemStatus.ACTIVE
                )
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        return itemMapper.toResponse(item);
    }

    @Override
    public ItemResponse getVendorItemById(UUID itemId, String authenticatedUserId) {
        Item item = getOwnedItem(itemId, authenticatedUserId);

        return itemMapper.toResponse(item);
    }

    @Override
    public PageResponse<ItemResponse> getPublicItems(ItemFilter filter, int pageNumber, int pageSize, String sortBy, String sortDirection) {
        Pageable pageable = createPageable(pageNumber, pageSize, sortBy, sortDirection);

        Specification<Item> specification = ItemSpecification.publicFilter(filter);

        Page<ItemResponse> page = itemRepository
                .findAll(specification, pageable)
                .map(itemMapper::toResponse);

        return PageResponse.from(page);
    }

    @Override
    public PageResponse<ItemResponse> getVendorItems(String authenticatedUserId, int pageNumber, int pageSize, String sortBy, String sortDirection) {
        Pageable pageable = createPageable(pageNumber, pageSize, sortBy, sortDirection);

        Specification<Item> specification = ItemSpecification.vendorOwnedItems(authenticatedUserId);

        Page<ItemResponse> page = itemRepository
                .findAll(specification, pageable)
                .map(itemMapper::toResponse);

        return PageResponse.from(page);
    }

    @Override
    public PageResponse<ItemResponse> getItemsByVendor(String ownerId, int pageNumber, int pageSize, String sortBy, String sortDirection) {
        Pageable pageable = createPageable(pageNumber, pageSize, sortBy, sortDirection);

        Specification<Item> specification = ItemSpecification.publicVendorItems(ownerId);

        Page<ItemResponse> page = itemRepository
                .findAll(specification, pageable)
                .map(itemMapper::toResponse);

        return PageResponse.from(page);
    }

    @Override
    @Transactional
    public ItemResponse updateItem(UUID itemId, UpdateItemRequest request, String authenticatedUserId) {
        Item item = getOwnedItem(itemId, authenticatedUserId);

        boolean hasImportantChanges = hasImportantChanges(request);

        itemMapper.updateEntity(
                request,
                item
        );

        /*
         * listing return to PENDING when important
         * listing information change.
         */
        if (
                hasImportantChanges
                        && item.getApprovalStatus()
                        == ItemApprovalStatus.APPROVED
        ) {
            item.setApprovalStatus(
                    ItemApprovalStatus.PENDING
            );

            item.setApprovedBy(null);
            item.setApprovedAt(null);
            item.setRejectionReason(null);
        }

        Item updatedItem = itemRepository.save(item);

        return itemMapper.toResponse(updatedItem);
    }

    @Override
    @Transactional
    public ItemResponse updateAvailability(UUID itemId, boolean available, String authenticatedUserId) {

        Item item = getOwnedItem(itemId, authenticatedUserId);
        item.setAvailable(available);
        Item updatedItem = itemRepository.save(item);

        return itemMapper.toResponse(updatedItem);
    }

    @Override
    @Transactional
    public ItemResponse updateStatus(UUID itemId, ItemStatus status, String authenticatedUserId) {

        Item item = getOwnedItem(itemId, authenticatedUserId);
        item.setStatus(status);
        Item updatedItem = itemRepository.save(item);

        return itemMapper.toResponse(updatedItem);
    }

    @Override
    @Transactional
    public void softDeleteItem(UUID itemId, String authenticatedUserId) {
        Item item = getOwnedItem(itemId, authenticatedUserId);

        item.setDeleted(true);
        item.setDeletedAt(OffsetDateTime.now());
        item.setAvailable(false);
        item.setStatus(ItemStatus.HIDDEN);

        itemRepository.save(item);
    }

    private Item getOwnedItem(UUID itemId, String authenticatedUserId) {
        Item item = itemRepository.findByIdAndDeletedFalse(itemId)
                .orElseThrow(
                        () -> new ItemNotFoundException(itemId)
                );

        if (!item.getOwnerId().equals(authenticatedUserId)) {
            throw new ItemAccessDeniedException();
        }

        return item;
    }

    private Pageable createPageable(int pageNumber, int pageSize, String sortBy, String sortDirection) {
        int safePageNumber = Math.max(pageNumber, 0);

        int safePageSize = Math.clamp(pageSize, 1, MAXIMUM_PAGE_SIZE);

        String safeSortField =
                validateSortField(sortBy);

        Sort.Direction direction =
                "asc".equalsIgnoreCase(sortDirection)
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        return PageRequest.of(safePageNumber, safePageSize,
                Sort.by(
                        direction,
                        safeSortField
                )
        );
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || !ALLOWED_SORT_FIELDS.contains(sortBy)) {
            return "createdAt";
        }

        return sortBy;
    }

    private boolean hasImportantChanges(UpdateItemRequest request) {
        return request.categoryId() != null
                || request.title() != null
                || request.description() != null
                || request.condition() != null
                || request.specifications() != null
                || request.locationText() != null
                || request.latitude() != null
                || request.longitude() != null
                || request.pricePerDay() != null
                || request.depositAmount() != null;
    }
}