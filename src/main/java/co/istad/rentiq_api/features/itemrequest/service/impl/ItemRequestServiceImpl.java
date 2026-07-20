package co.istad.rentiq_api.features.itemrequest.service.impl;

import co.istad.rentiq_api.exception.IllegalItemRequestStateException;
import co.istad.rentiq_api.exception.ItemRequestAccessDeniedException;
import co.istad.rentiq_api.exception.ItemRequestNotFoundException;
import co.istad.rentiq_api.features.item.dto.respone.PageResponse;
import co.istad.rentiq_api.features.itemrequest.dto.request.*;
import co.istad.rentiq_api.features.itemrequest.dto.response.ItemRequestResponse;
import co.istad.rentiq_api.features.itemrequest.entity.ItemRequest;
import co.istad.rentiq_api.features.itemrequest.enums.ItemRequestStatus;
import co.istad.rentiq_api.features.itemrequest.mapper.ItemRequestMapper;
import co.istad.rentiq_api.features.itemrequest.repository.ItemRequestRepository;
import co.istad.rentiq_api.features.itemrequest.service.ItemRequestService;
import co.istad.rentiq_api.features.itemrequest.specification.ItemRequestSpecification;
import co.istad.rentiq_api.features.itemrequest.utils.GeographyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemRequestServiceImpl
        implements ItemRequestService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of(
                    "createdAt",
                    "neededFrom",
                    "neededTo",
                    "budgetMin",
                    "budgetMax",
                    "expiresAt",
                    "title"
            );

    private final ItemRequestRepository itemRequestRepository;
    private final ItemRequestMapper itemRequestMapper;

    @Override
    @Transactional
    public ItemRequestResponse create(CreateItemRequestRequest request, String authenticatedUserId) {
        ItemRequest entity = itemRequestMapper.toEntity(
                request,
                authenticatedUserId
        );

        return itemRequestMapper.toResponse(itemRequestRepository.save(entity));
    }

    @Override
    public PageResponse<ItemRequestResponse> getOpenRequests(ItemRequestFilter filter) {
        Pageable pageable = createPageable(
                filter.pageNumber(),
                filter.pageSize(),
                filter.sortBy(),
                filter.sortDirection()
        );

        Page<ItemRequestResponse> response =
                itemRequestRepository.findAll(ItemRequestSpecification
                                .publicFilter(filter), pageable)
                        .map(itemRequestMapper::toResponse);

        return PageResponse.from(response);
    }

    @Override
    public ItemRequestResponse getById(UUID requestId, String authenticatedUserId) {
        ItemRequest request = findRequest(requestId);

        expireIfNecessary(request);

        boolean owner = authenticatedUserId != null && request.getCustomerId()
                .equals(authenticatedUserId);

        if (request.getStatus() != ItemRequestStatus.OPEN && !owner) {
            throw new ItemRequestAccessDeniedException();
        }

        return itemRequestMapper.toResponse(request);
    }

    @Override
    @Transactional
    public ItemRequestResponse update(UUID requestId, UpdateItemRequestRequest update, String authenticatedUserId) {
        ItemRequest request = getOwnedRequest(requestId, authenticatedUserId);

        if (request.getStatus() != ItemRequestStatus.OPEN) {
            throw new IllegalItemRequestStateException(
                    "Only an OPEN request can be updated"
            );
        }

        applyUpdate(request, update);
        validateFinalValues(request);

        return itemRequestMapper.toResponse(itemRequestRepository.save(request));
    }

    @Override
    @Transactional
    public void cancel(UUID requestId, String authenticatedUserId) {
        ItemRequest request = getOwnedRequest(requestId, authenticatedUserId);

        if (request.getStatus() != ItemRequestStatus.OPEN) {
            throw new IllegalItemRequestStateException(
                    "Only an OPEN request can be cancelled"
            );
        }

        request.setStatus(ItemRequestStatus.CANCELLED);
        itemRequestRepository.save(request);
    }

    @Override
    public PageResponse<ItemRequestResponse> getMyRequests(String authenticatedUserId, Integer pageNumber, Integer pageSize) {
        Pageable pageable = PageRequest.of(
                normalizePageNumber(pageNumber),
                normalizePageSize(pageSize),
                Sort.by("createdAt").descending()
        );

        Page<ItemRequestResponse> response = itemRequestRepository
                        .findAllByCustomerIdOrderByCreatedAtDesc(
                                authenticatedUserId,
                                pageable
                        )
                        .map(itemRequestMapper::toResponse);

        return PageResponse.from(response);
    }

    @Override
    public PageResponse<ItemRequestResponse> getNearbyRequests(NearbyItemRequestFilter filter) {
        Pageable pageable = PageRequest.of(filter.pageNumber(), filter.pageSize());

        Page<ItemRequestResponse> response = itemRequestRepository
                        .findNearbyOpenRequests(
                                filter.latitude(),
                                filter.longitude(),
                                filter.radiusKm(),
                                filter.categoryId(),
                                pageable
                        )
                        .map(itemRequestMapper::toResponse);

        return PageResponse.from(response);
    }

    private ItemRequest findRequest(UUID requestId) {
        return itemRequestRepository.findById(requestId)
                .orElseThrow(
                        () -> new ItemRequestNotFoundException(requestId)
                );
    }

    private ItemRequest getOwnedRequest(UUID requestId, String authenticatedUserId) {
        ItemRequest request = findRequest(requestId);

        if (authenticatedUserId == null || !request.getCustomerId().equals(authenticatedUserId)) {
            throw new ItemRequestAccessDeniedException();
        }

        return request;
    }

    private void applyUpdate(ItemRequest entity, UpdateItemRequestRequest update) {
        if (update.categoryId() != null) {
            entity.setCategoryId(update.categoryId());
        }

        if (update.title() != null) {
            entity.setTitle(update.title());
        }

        if (update.description() != null) {
            entity.setDescription(update.description());
        }

        if (update.budgetMin() != null) {
            entity.setBudgetMin(update.budgetMin());
        }

        if (update.budgetMax() != null) {
            entity.setBudgetMax(update.budgetMax());
        }

        if (update.neededFrom() != null) {
            entity.setNeededFrom(update.neededFrom());
        }

        if (update.neededTo() != null) {
            entity.setNeededTo(update.neededTo());
        }

        if (update.latitude() != null && update.longitude() != null) {
            entity.setLocation(
                    GeographyUtils.createPoint(
                            update.latitude(),
                            update.longitude()
                    )
            );
        }

        if (update.radiusKm() != null) {
            entity.setRadiusKm(update.radiusKm());
        }

        if (update.expiresAt() != null) {
            entity.setExpiresAt(update.expiresAt());
        }
    }

    private void validateFinalValues(ItemRequest request) {
        if (request.getBudgetMin() != null && request.getBudgetMax() != null && request.getBudgetMax()
                .compareTo(request.getBudgetMin()) < 0) {
            throw new IllegalItemRequestStateException(
                    "Maximum budget cannot be less than minimum budget"
            );
        }

        if (request.getNeededFrom() != null && request.getNeededTo() != null && request.getNeededTo()
                .isBefore(request.getNeededFrom())) {
            throw new IllegalItemRequestStateException(
                    "Needed-to date cannot be before needed-from date"
            );
        }

        if (request.getNeededFrom() != null && request.getNeededFrom().isBefore(LocalDate.now())) {
            throw new IllegalItemRequestStateException(
                    "Needed-from date cannot be in the past"
            );
        }
    }

    private void expireIfNecessary(ItemRequest request) {
        if (request.getStatus() == ItemRequestStatus.OPEN && request.getExpiresAt() != null && request.getExpiresAt()
                .isBefore(OffsetDateTime.now())) {
            request.setStatus(ItemRequestStatus.EXPIRED);
        }
    }

    private Pageable createPageable(Integer pageNumber, Integer pageSize, String sortBy, String sortDirection) {
        String safeSortField = ALLOWED_SORT_FIELDS.contains(sortBy)
                        ? sortBy
                        : "createdAt";

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        return PageRequest.of(
                normalizePageNumber(pageNumber),
                normalizePageSize(pageSize),
                Sort.by(direction, safeSortField)
        );
    }

    private int normalizePageNumber(Integer pageNumber) {
        return pageNumber == null
                ? 0
                : Math.max(pageNumber, 0);
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
}