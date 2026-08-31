package co.istad.rentiq_api.features.item.service;

import co.istad.rentiq_api.features.item.dto.request.CreateItemRequest;
import co.istad.rentiq_api.features.item.dto.request.ItemFilter;
import co.istad.rentiq_api.features.item.dto.request.AdminItemFilter;
import co.istad.rentiq_api.features.item.dto.respone.AdminItemResponse;
import co.istad.rentiq_api.features.item.dto.request.NearbyItemFilter;
import co.istad.rentiq_api.features.item.dto.request.UpdateItemRequest;
import co.istad.rentiq_api.features.item.dto.respone.ItemResponse;
import co.istad.rentiq_api.features.item.dto.respone.PageResponse;
import co.istad.rentiq_api.features.item.enums.ItemAvailabilityState;
import co.istad.rentiq_api.features.item.enums.ItemStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface ItemService {
    ItemResponse createItem(CreateItemRequest request, String authenticatedUserId);
    ItemResponse getPublicItemById(UUID itemId);
    ItemResponse getVendorItemById(UUID itemId, String authenticatedUserId);
    PageResponse<ItemResponse> getPublicItems(ItemFilter filter, int pageNumber, int pageSize, String sortBy, String sortDirection);
    PageResponse<ItemResponse> getVendorItems(String authenticatedUserId, int pageNumber, int pageSize, String sortBy, String sortDirection);
    PageResponse<ItemResponse> getItemsByVendor(String ownerId, int pageNumber, int pageSize, String sortBy, String sortDirection);
    PageResponse<ItemResponse> getFeaturedItems(int pageNumber, int pageSize);
    PageResponse<ItemResponse> getNearbyItems(NearbyItemFilter filter);
    ItemResponse updateItem(UUID itemId, UpdateItemRequest request, String authenticatedUserId);
    ItemResponse updateAvailability(UUID itemId, ItemAvailabilityState availability, String authenticatedUserId);
    ItemResponse updateStatus(UUID itemId, ItemStatus status, String authenticatedUserId);
    void softDeleteItem(UUID itemId, String authenticatedUserId);

    PageResponse<AdminItemResponse> getAdminItems(AdminItemFilter filter, int pageNumber, int pageSize, String sortBy, String sortDirection);
    AdminItemResponse getAdminItemById(UUID itemId);

    ItemResponse adminApproveItem(UUID itemId, String adminId);
    ItemResponse adminRejectItem(UUID itemId, String reason, String adminId);
    void adminRemoveItem(UUID itemId);
    ItemResponse adminSetFeatured(UUID itemId, boolean featured, OffsetDateTime featuredUntil);
}
