package co.istad.rentiq_api.features.itemrequest.service.impl;

import co.istad.rentiq_api.common.exception.*;
import co.istad.rentiq_api.features.item.dto.respone.PageResponse;
import co.istad.rentiq_api.features.item.entity.Item;
import co.istad.rentiq_api.features.item.repository.ItemRepository;
import co.istad.rentiq_api.features.itemrequest.dto.request.CreateOfferRequest;
import co.istad.rentiq_api.features.itemrequest.dto.request.UpdateOfferRequest;
import co.istad.rentiq_api.features.itemrequest.dto.response.OfferResponse;
import co.istad.rentiq_api.features.itemrequest.dto.response.OfferStatusResponse;
import co.istad.rentiq_api.features.itemrequest.entity.ItemRequest;
import co.istad.rentiq_api.features.itemrequest.entity.Offer;
import co.istad.rentiq_api.features.itemrequest.enums.ItemRequestStatus;
import co.istad.rentiq_api.features.itemrequest.enums.OfferStatus;
import co.istad.rentiq_api.features.itemrequest.mapper.OfferMapper;
import co.istad.rentiq_api.features.itemrequest.repository.ItemRequestRepository;
import co.istad.rentiq_api.features.itemrequest.repository.OfferRepository;
import co.istad.rentiq_api.features.itemrequest.service.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfferServiceImpl implements OfferService {

    private final ItemRequestRepository itemRequestRepository;
    private final OfferRepository offerRepository;
    private final ItemRepository itemRepository;
    private final OfferMapper offerMapper;

    @Override
    @Transactional
    public OfferResponse createOffer(UUID requestId, CreateOfferRequest request, String authenticatedOwnerId) {
        ItemRequest itemRequest = getOpenRequest(requestId);

        if (authenticatedOwnerId == null || authenticatedOwnerId.isBlank()) {
            throw new ForbiddenException("Offer", "An authenticated vendor is required");
        }

        if (itemRequest.getCustomerId()
                .equals(authenticatedOwnerId)) {
            throw new InvalidOperationException("Offer", "A customer cannot submit an offer to their own request");
        }

        boolean offerAlreadyExists = offerRepository
                        .existsByItemRequestIdAndOwnerId(
                                requestId,
                                authenticatedOwnerId
                        );

        if (offerAlreadyExists) {
            throw new DuplicateException(
                    "Offer", "You have already submitted an offer for this item request"
            );
        }

        Item item = resolveOwnedItem(request.itemId(), authenticatedOwnerId);

        Offer offer = Offer.builder()
                .itemRequest(itemRequest)
                .ownerId(authenticatedOwnerId)
                .item(item)
                .offeredPrice(request.offeredPrice())
                .currency(
                        request.currency() == null ? "USD" : request.currency().trim().toUpperCase()
                )
                .message(
                        request.message() == null ? null : request.message().trim()
                )
                .status(OfferStatus.PENDING)
                .build();

        Offer savedOffer = offerRepository.save(offer);

        return offerMapper.toResponse(savedOffer);
    }

    @Override
    public List<OfferResponse> getRequestOffers(UUID requestId, String authenticatedCustomerId) {
        ItemRequest request = getOwnedRequest(requestId, authenticatedCustomerId);

        return offerRepository.findAllByItemRequestIdOrderByCreatedAtDesc(request.getId())
                .stream()
                .map(offerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public OfferResponse updateOffer(UUID offerId, UpdateOfferRequest update, String authenticatedOwnerId) {
        Offer offer = getOwnedOffer(offerId, authenticatedOwnerId);

        ItemRequest itemRequest = offer.getItemRequest();

        if (itemRequest.getStatus() != ItemRequestStatus.OPEN) {
            throw new InvalidStateException(
                    "Item request",
                    itemRequest.getStatus(),
                    "The offer cannot be edited because the item request is no longer OPEN"
            );
        }

        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new InvalidStateException(
                    "Offer",
                    offer.getStatus(),
                    "Only a PENDING offer can be edited"
            );
        }

        if (update.itemId() != null) {
            offer.setItem(
                    resolveOwnedItem(
                            update.itemId(),
                            authenticatedOwnerId
                    )
            );
        }

        if (update.offeredPrice() != null) {
            offer.setOfferedPrice(update.offeredPrice());
        }

        if (update.currency() != null) {
            offer.setCurrency(update.currency());
        }

        if (update.message() != null) {
            offer.setMessage(update.message());
        }

        return offerMapper.toResponse(
                offerRepository.save(offer)
        );
    }

    @Override
    @Transactional
    public void withdrawOffer(UUID offerId, String authenticatedOwnerId) {
        Offer offer = getOwnedOffer(offerId, authenticatedOwnerId);

        ItemRequest itemRequest = offer.getItemRequest();

        if (itemRequest.getStatus() != ItemRequestStatus.OPEN) {
            throw new InvalidStateException(
                    "Item request",
                    itemRequest.getStatus(),
                    "The offer cannot be withdrawn because the request is no longer OPEN"
            );
        }

        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new InvalidStateException(
                    "Offer",
                    offer.getStatus(),
                    "Only a PENDING offer can be withdrawn"
            );
        }

        offer.setStatus(OfferStatus.WITHDRAWN);
        offerRepository.save(offer);
    }

    private Offer getOwnedOffer(UUID offerId, String authenticatedOwnerId) {
        return offerRepository
                .findByIdAndOwnerId(offerId, authenticatedOwnerId)
                .orElseThrow(
                        () -> new NotFoundException("Offer id not found" +offerId)
                );
    }

    @Override
    public OfferStatusResponse getMyOfferStatus(UUID offerId, String authenticatedOwnerId) {
        Offer offer = getOwnedOffer(offerId, authenticatedOwnerId);

        return new OfferStatusResponse(
                offer.getId(),
                offer.getItemRequest().getId(),
                offer.getStatus(),
                offer.getCreatedAt(),
                offer.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public OfferResponse acceptOffer(UUID requestId, UUID offerId, String authenticatedCustomerId) {
        ItemRequest request = getOwnedRequest(requestId, authenticatedCustomerId);

        if (request.getStatus() != ItemRequestStatus.OPEN) {
            throw new InvalidStateException(
                    "Item request",
                    request.getStatus(),
                    "Only an OPEN request can accept an offer"
            );
        }

        Offer selectedOffer = getOffer(requestId, offerId);

        if (selectedOffer.getStatus() != OfferStatus.PENDING) {
            throw new InvalidStateException(
                    "Offer",
                    selectedOffer.getStatus(),
                    "Only a PENDING offer can be accepted"
            );
        }

        List<Offer> pendingOffers = offerRepository
                        .findAllByItemRequestIdAndStatus(
                                requestId,
                                OfferStatus.PENDING
                        );

        for (Offer offer : pendingOffers) {
            if (offer.getId().equals(offerId)) {
                offer.setStatus(
                        OfferStatus.ACCEPTED
                );
            } else {
                offer.setStatus(
                        OfferStatus.REJECTED
                );
            }
        }

        offerRepository.saveAll(pendingOffers);

        request.setStatus(ItemRequestStatus.MATCHED);
        itemRequestRepository.save(request);

        return offerMapper.toResponse(selectedOffer);
    }

    @Override
    @Transactional
    public OfferResponse rejectOffer(UUID requestId, UUID offerId, String authenticatedCustomerId) {
        ItemRequest request = getOwnedRequest(requestId, authenticatedCustomerId);
        if (request.getStatus() != ItemRequestStatus.OPEN) {
            throw new InvalidStateException(
                    "Item request",
                    request.getStatus(),
                    "Offers can only be rejected while the request is OPEN"
            );
        }

        Offer offer = getOffer(requestId, offerId);

        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new InvalidStateException(
                    "Offer",
                    offer.getStatus(),
                    "Only a PENDING offer can be rejected"
            );
        }

        offer.setStatus(OfferStatus.REJECTED);
        Offer savedOffer = offerRepository.save(offer);

        return offerMapper.toResponse(savedOffer);
    }


    @Override
    public PageResponse<OfferResponse> getMyOffers(
            String authenticatedOwnerId,
            Integer pageNumber,
            Integer pageSize
    ) {
        Pageable pageable = PageRequest.of(
                pageNumber == null
                        ? 0
                        : Math.max(pageNumber, 0),
                pageSize == null
                        ? 20
                        : Math.max(1, Math.min(pageSize, 100)),
                Sort.by("createdAt").descending()
        );

        Page<OfferResponse> response = offerRepository
                        .findAllByOwnerIdOrderByCreatedAtDesc(
                                authenticatedOwnerId,
                                pageable
                        )
                        .map(offerMapper::toResponse);

        return PageResponse.from(response);
    }

    private ItemRequest getOpenRequest(UUID requestId) {
        ItemRequest request = itemRequestRepository
                        .findById(requestId)
                        .orElseThrow(
                                () -> new NotFoundException("Item request", requestId)
                        );

        if (request.getStatus() != ItemRequestStatus.OPEN) {
            throw new InvalidStateException(
                    "Item request",
                    request.getStatus(),
                    "Offers can only be submitted for OPEN requests"
            );
        }

        return request;
    }

    private ItemRequest getOwnedRequest(UUID requestId, String authenticatedCustomerId) {
        ItemRequest request = itemRequestRepository
                        .findById(requestId)
                        .orElseThrow(
                                () -> new NotFoundException("Item request", requestId)
                        );

        if (authenticatedCustomerId == null || !request.getCustomerId()
                .equals(authenticatedCustomerId)) {
            throw new ForbiddenException(
                    "Item request", "Only the request owner can perform this operation");
        }

        return request;
    }


    private Offer getOffer(UUID requestId, UUID offerId) {
        return offerRepository
                .findByIdAndItemRequestId(offerId, requestId)
                .orElseThrow(
                        () -> new NotFoundException("Item request", requestId)
                );
    }

    private void verifyOfferOwner(Offer offer, String authenticatedOwnerId) {
        if (!offer.getOwnerId()
                .equals(authenticatedOwnerId)) {
            throw new ForbiddenException(
                    "Item request",
                    "Only the request owner can perform this operation"
            );
        }
    }

    private Item resolveOwnedItem(UUID itemId, String authenticatedOwnerId) {
        if (itemId == null) {
            return null;
        }

        Item item = itemRepository.findByIdAndDeletedFalse(itemId).orElseThrow(
                () -> new NotFoundException("Item", itemId)
                );

        if (!item.getOwnerId().equals(authenticatedOwnerId)) {
            throw new ForbiddenException(
                    "Item", "Only the item owner can include this item in an offer"
            );
        }

        return item;
    }

    @Override
    public OfferResponse getOfferById(UUID offerId, String authenticatedOwnerId) {
        Offer offer = getOwnedOffer(offerId, authenticatedOwnerId);

        return offerMapper.toResponse(offer);
    }
}
