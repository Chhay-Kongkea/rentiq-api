package co.istad.rentiq_api.features.itemrequest.service;

import co.istad.rentiq_api.features.item.dto.respone.PageResponse;
import co.istad.rentiq_api.features.itemrequest.dto.request.*;
import co.istad.rentiq_api.features.itemrequest.dto.response.ItemRequestResponse;

import java.util.UUID;

public interface ItemRequestService {

    ItemRequestResponse create(CreateItemRequestRequest request, String authenticatedUserId);
    PageResponse<ItemRequestResponse> getOpenRequests(ItemRequestFilter filter);
    ItemRequestResponse getById(UUID requestId, String authenticatedUserId);
    ItemRequestResponse update(UUID requestId, UpdateItemRequestRequest request, String authenticatedUserId);

    void cancel(UUID requestId, String authenticatedUserId);

    PageResponse<ItemRequestResponse> getMyRequests(String authenticatedUserId, Integer pageNumber, Integer pageSize);
    PageResponse<ItemRequestResponse> getNearbyRequests(NearbyItemRequestFilter filter);
}
