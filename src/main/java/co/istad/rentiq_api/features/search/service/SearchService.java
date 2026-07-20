package co.istad.rentiq_api.features.search.service;

import co.istad.rentiq_api.features.item.dto.respone.ItemResponse;
import co.istad.rentiq_api.features.item.dto.respone.PageResponse;
import co.istad.rentiq_api.features.search.dto.request.ItemSearchFilter;
import co.istad.rentiq_api.features.search.dto.request.NearbyItemSearchFilter;
import co.istad.rentiq_api.features.search.dto.respone.SearchLogResponse;
import co.istad.rentiq_api.features.search.dto.respone.SearchSuggestionResponse;


import java.util.List;

public interface SearchService {
    PageResponse<ItemResponse> searchItems(ItemSearchFilter filter, String authenticatedUserId);
    List<SearchSuggestionResponse> getSuggestions(String keyword, Integer limit);
    PageResponse<ItemResponse> searchNearby(NearbyItemSearchFilter filter, String authenticatedUserId);
    PageResponse<SearchLogResponse> getMySearchLogs(String authenticatedUserId, Integer pageNumber, Integer pageSize);
    PageResponse<SearchLogResponse> getAllSearchLogs(Integer pageNumber, Integer pageSize);
}