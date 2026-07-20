package co.istad.rentiq_api.features.itemrequest.service.impl;

import co.istad.rentiq_api.exception.*;
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

        if (itemRequest.getCustomerId().equals(authenticatedOwnerId)) {
            throw new IllegalOfferStateException(
                    "A customer cannot submit an offer to their own request"
            );
        }

        if (offerRepository
                .existsByItemRequestIdAndOwnerId(requestId, authenticatedOwnerId)) {
            throw new DuplicateOfferException();
        }

        Item item = resolveOwnedItem(
                request.itemId(),
                authenticatedOwnerId
        );

        Offer offer = Offer.builder()
                .itemRequest(itemRequest)
                .ownerId(authenticatedOwnerId)
                .item(item)
                .offeredPrice(request.offeredPrice())
                .currency(request.currency())
                .message(request.message())
                .status(OfferStatus.PENDING)
                .build();

        return offerMapper.toResponse(offerRepository.save(offer));
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
            throw new IllegalItemRequestStateException(
                    "The offer cannot be edited because the item request is no longer OPEN"
            );
        }

        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new IllegalOfferStateException(
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

        if (offer.getItemRequest().getStatus() != ItemRequestStatus.OPEN) {
            throw new IllegalItemRequestStateException(
                    "The offer cannot be withdrawn because the request is no longer OPEN"
            );
        }

        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new IllegalOfferStateException(
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
                        () -> new OfferNotFoundException(offerId)
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
            throw new IllegalItemRequestStateException(
                    "Only an OPEN request can accept an offer"
            );
        }

        Offer selectedOffer = getOffer(requestId, offerId);

        if (selectedOffer.getStatus()
                != OfferStatus.PENDING) {
            throw new IllegalOfferStateException(
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
                offer.setStatus(OfferStatus.ACCEPTED);
            } else {
                offer.setStatus(OfferStatus.REJECTED);
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
            throw new IllegalItemRequestStateException(
                    "Offers can only be rejected while the request is OPEN"
            );
        }

        Offer offer = getOffer(requestId, offerId);

        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new IllegalOfferStateException(
                    "Only a PENDING offer can be rejected"
            );
        }

        offer.setStatus(OfferStatus.REJECTED);

        return offerMapper.toResponse(offerRepository.save(offer));
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
        ItemRequest request = itemRequestRepository.findById(requestId)
                .orElseThrow(
                        () -> new ItemRequestNotFoundException(requestId)
                );

        if (request.getStatus() != ItemRequestStatus.OPEN) {
            throw new IllegalItemRequestStateException(
                    "Offers can only be submitted for OPEN requests"
            );
        }

        return request;
    }

    private ItemRequest getOwnedRequest(UUID requestId, String authenticatedCustomerId) {
        ItemRequest request = itemRequestRepository.findById(requestId)
                .orElseThrow(
                        () -> new ItemRequestNotFoundException(
                                requestId
                        )
                );

        if (!request.getCustomerId().equals(authenticatedCustomerId)) {
            throw new ItemRequestAccessDeniedException();
        }

        return request;
    }

    private Offer getOffer(UUID requestId, UUID offerId) {
        return offerRepository
                .findByIdAndItemRequestId(offerId, requestId)
                .orElseThrow(
                        () -> new OfferNotFoundException(
                                offerId
                        )
                );
    }

    private void verifyOfferOwner(Offer offer, String authenticatedOwnerId) {
        if (!offer.getOwnerId()
                .equals(authenticatedOwnerId)) {
            throw new OfferAccessDeniedException();
        }
    }

    private Item resolveOwnedItem(UUID itemId, String authenticatedOwnerId) {
        if (itemId == null) {
            return null;
        }

        Item item = itemRepository.findByIdAndDeletedFalse(itemId).orElseThrow(
                        () -> new ItemNotFoundException(itemId)
                );

        if (!item.getOwnerId().equals(authenticatedOwnerId)) {
            throw new OfferAccessDeniedException();
        }

        return item;
    }

    @Override
    public OfferResponse getOfferById(UUID offerId, String authenticatedOwnerId) {
        Offer offer = getOwnedOffer(offerId, authenticatedOwnerId);

        return offerMapper.toResponse(offer);
    }
}
