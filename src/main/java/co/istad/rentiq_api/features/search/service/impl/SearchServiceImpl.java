package co.istad.rentiq_api.features.search.service.impl;

import co.istad.rentiq_api.features.item.dto.respone.ItemResponse;
import co.istad.rentiq_api.features.item.dto.respone.PageResponse;
import co.istad.rentiq_api.features.item.entity.Item;
import co.istad.rentiq_api.features.item.mapper.ItemMapper;
import co.istad.rentiq_api.features.item.repository.ItemRepository;
import co.istad.rentiq_api.features.itemrequest.utils.GeographyUtils;
import co.istad.rentiq_api.features.search.dto.request.ItemSearchFilter;
import co.istad.rentiq_api.features.search.dto.request.NearbyItemSearchFilter;
import co.istad.rentiq_api.features.search.dto.respone.SearchLogResponse;
import co.istad.rentiq_api.features.search.dto.respone.SearchSuggestionResponse;
import co.istad.rentiq_api.features.search.entity.SearchLog;
import co.istad.rentiq_api.features.search.mapper.SearchLogMapper;
import co.istad.rentiq_api.features.search.repository.SearchLogRepository;
import co.istad.rentiq_api.features.search.service.SearchService;
import co.istad.rentiq_api.features.search.specification.SearchItemSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchServiceImpl implements SearchService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of(
                    "title",
                    "pricePerDay",
                    "averageRating",
                    "totalReviews",
                    "totalBookings",
                    "viewCount",
                    "favoriteCount",
                    "createdAt",
                    "updatedAt"
            );

    private final ItemRepository itemRepository;
    private final SearchLogRepository searchLogRepository;
    private final ItemMapper itemMapper;
    private final SearchLogMapper searchLogMapper;

    @Override
    @Transactional
    public PageResponse<ItemResponse> searchItems(
            ItemSearchFilter filter,
            String authenticatedUserId
    ) {
        Pageable pageable = createPageable(
                filter.pageNumber(),
                filter.pageSize(),
                filter.sortBy(),
                filter.sortDirection()
        );

        Page<Item> itemPage = itemRepository.findAll(
                SearchItemSpecification.build(filter),
                pageable
        );

        saveSearchLog(
                authenticatedUserId,
                filter.keyword(),
                filter.categoryId(),
                null,
                null
        );

        Page<ItemResponse> response =
                itemPage.map(itemMapper::toResponse);

        return PageResponse.from(response);
    }

    @Override
    public List<SearchSuggestionResponse> getSuggestions(
            String keyword,
            Integer limit
    ) {
        String normalizedKeyword =
                keyword == null
                        ? ""
                        : keyword.trim();

        if (normalizedKeyword.length() < 2) {
            return List.of();
        }

        int safeLimit = limit == null
                ? 10
                : Math.max(1, Math.min(limit, 20));

        Pageable pageable = PageRequest.of(
                0,
                safeLimit
        );

        return itemRepository
                .findTitleSuggestions(
                        normalizedKeyword,
                        pageable
                )
                .stream()
                .map(title ->
                        new SearchSuggestionResponse(
                                title,
                                "ITEM"
                        )
                )
                .toList();
    }

    @Override
    @Transactional
    public PageResponse<ItemResponse> searchNearby(
            NearbyItemSearchFilter filter,
            String authenticatedUserId
    ) {
        Pageable pageable = PageRequest.of(
                normalizePageNumber(filter.pageNumber()),
                normalizePageSize(filter.pageSize())
        );

        Page<Item> itemPage =
                itemRepository.searchNearby(
                        filter.latitude(),
                        filter.longitude(),
                        filter.radiusKm(),
                        filter.categoryId(),
                        pageable
                );

        saveSearchLog(
                authenticatedUserId,
                null,
                filter.categoryId(),
                filter.latitude(),
                filter.longitude()
        );

        Page<ItemResponse> response =
                itemPage.map(itemMapper::toResponse);

        return PageResponse.from(response);
    }

    @Override
    public PageResponse<SearchLogResponse> getMySearchLogs(
            String authenticatedUserId,
            Integer pageNumber,
            Integer pageSize
    ) {
        Pageable pageable = PageRequest.of(
                normalizePageNumber(pageNumber),
                normalizePageSize(pageSize),
                Sort.by("createdAt").descending()
        );

        Page<SearchLogResponse> response =
                searchLogRepository
                        .findAllByUserIdOrderByCreatedAtDesc(
                                authenticatedUserId,
                                pageable
                        )
                        .map(searchLogMapper::toResponse);

        return PageResponse.from(response);
    }

    @Override
    public PageResponse<SearchLogResponse> getAllSearchLogs(
            Integer pageNumber,
            Integer pageSize
    ) {
        Pageable pageable = PageRequest.of(
                normalizePageNumber(pageNumber),
                normalizePageSize(pageSize),
                Sort.by("createdAt").descending()
        );

        Page<SearchLogResponse> response =
                searchLogRepository
                        .findAll(pageable)
                        .map(searchLogMapper::toResponse);

        return PageResponse.from(response);
    }

    private void saveSearchLog(
            String userId,
            String keyword,
            Short categoryId,
            Double latitude,
            Double longitude
    ) {
        SearchLog.SearchLogBuilder builder =
                SearchLog.builder()
                        .userId(userId)
                        .keyword(normalize(keyword))
                        .categoryId(categoryId);

        if (latitude != null && longitude != null) {
            builder.location(
                    GeographyUtils.createPoint(
                            latitude,
                            longitude
                    )
            );
        }

        searchLogRepository.save(builder.build());
    }

    private Pageable createPageable(
            Integer pageNumber,
            Integer pageSize,
            String sortBy,
            String sortDirection
    ) {
        String safeSortField =
                ALLOWED_SORT_FIELDS.contains(sortBy)
                        ? sortBy
                        : "createdAt";

        Sort.Direction direction =
                "asc".equalsIgnoreCase(sortDirection)
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        return PageRequest.of(
                normalizePageNumber(pageNumber),
                normalizePageSize(pageSize),
                Sort.by(direction, safeSortField)
        );
    }

    private int normalizePageNumber(Integer pageNumber) {
        if (pageNumber == null) {
            return 0;
        }

        return Math.max(pageNumber, 0);
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return 12;
        }

        return Math.max(
                1,
                Math.min(pageSize, MAX_PAGE_SIZE)
        );
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}