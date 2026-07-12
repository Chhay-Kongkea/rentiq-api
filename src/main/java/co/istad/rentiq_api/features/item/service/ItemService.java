package co.istad.rentiq_api.features.item.service;

import co.istad.rentiq_api.features.item.dto.respone.PageResponse;
import co.istad.rentiq_api.features.item.dto.request.CreateItemRequest;
import co.istad.rentiq_api.features.item.dto.request.ItemFilter;
import co.istad.rentiq_api.features.item.dto.request.UpdateItemRequest;
import co.istad.rentiq_api.features.item.dto.respone.ItemResponse;
import co.istad.rentiq_api.features.item.enums.ItemStatus;

import java.util.UUID;

public interface ItemService {
    ItemResponse createItem(CreateItemRequest request, String authenticatedUserId);
    ItemResponse getPublicItemById(UUID itemId);
    ItemResponse getVendorItemById(UUID itemId, String authenticatedUserId);
    PageResponse<ItemResponse> getPublicItems(ItemFilter filter, int pageNumber, int pageSize, String sortBy, String sortDirection);
    PageResponse<ItemResponse> getVendorItems(String authenticatedUserId, int pageNumber, int pageSize, String sortBy, String sortDirection);
    PageResponse<ItemResponse> getItemsByVendor(String ownerId, int pageNumber, int pageSize, String sortBy, String sortDirection);
    ItemResponse updateItem(UUID itemId, UpdateItemRequest request, String authenticatedUserId);
    ItemResponse updateAvailability(UUID itemId, boolean available, String authenticatedUserId);
    ItemResponse updateStatus(UUID itemId, ItemStatus status, String authenticatedUserId);
    void softDeleteItem(UUID itemId, String authenticatedUserId);
}