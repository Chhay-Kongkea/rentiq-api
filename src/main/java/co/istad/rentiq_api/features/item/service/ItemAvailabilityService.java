package co.istad.rentiq_api.features.item.service;

import co.istad.rentiq_api.features.item.dto.request.CreateAvailabilityBlockRequest;
import co.istad.rentiq_api.features.item.dto.respone.AvailabilityBlockResponse;

import java.util.List;
import java.util.UUID;

public interface ItemAvailabilityService {
    List<AvailabilityBlockResponse> getBlockedRanges(UUID itemId);
    AvailabilityBlockResponse createBlock(UUID itemId, CreateAvailabilityBlockRequest request, String authenticatedUserId);
    void deleteBlock(UUID itemId, UUID blockId, String authenticatedUserId);
}
