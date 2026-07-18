package co.istad.rentiq_api.features.offers.service.impl;


import co.istad.rentiq_api.features.item.entity.Item;
import co.istad.rentiq_api.features.item.repository.ItemRepository;
import co.istad.rentiq_api.features.offers.dto.response.OfferStatusResponse;
import co.istad.rentiq_api.features.offers.entity.Offer;
import co.istad.rentiq_api.features.offers.enums.OfferStatus;
import co.istad.rentiq_api.features.offers.mapper.OfferMapper;
import co.istad.rentiq_api.features.offers.dto.request.CreateOfferRequest;
import co.istad.rentiq_api.features.offers.dto.request.UpdateOfferRequest;
import co.istad.rentiq_api.features.offers.dto.response.OfferResponse;
import co.istad.rentiq_api.features.offers.repository.OfferRepository;
import co.istad.rentiq_api.features.offers.service.OfferService;
import co.istad.rentiq_api.exception.ItemNotFoundException;
import co.istad.rentiq_api.features.offers.exception.OfferNotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
@Service
@RequiredArgsConstructor
@Transactional
public class OfferServiceImpl implements OfferService {

    private final OfferRepository offerRepository;
    private final ItemRepository itemRepository;
    private final OfferMapper mapper;

    @Override
    public OfferResponse createOffer(UUID requestId,
                                     CreateOfferRequest request,
                                     String ownerId) {

        Item item = itemRepository.findById(request.itemId())
                .orElseThrow(() ->
                        new ItemNotFoundException(request.itemId()));

        Offer offer = mapper.toEntity(request);

        offer.setItem(item);
        offer.setRequesterId(ownerId);
        offer.setVendorId(item.getOwnerId());
        offer.setStatus(OfferStatus.PENDING);

        Offer saved = offerRepository.save(offer);

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OfferResponse getOffer(UUID id) {

        Offer offer = offerRepository.findById(id)
                .orElseThrow(() ->
                        new OfferNotFoundException(id));

        return mapper.toResponse(offer);
    }

    @Override
    public OfferResponse updateOffer(UUID id,
                                     UpdateOfferRequest request,
                                     String ownerId) {

        Offer offer = offerRepository.findById(id)
                .orElseThrow(() ->
                        new OfferNotFoundException(id));

        if (!offer.getRequesterId().equals(ownerId)) {
            throw new RuntimeException("Access denied");
        }

        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new RuntimeException("Offer cannot be edited.");
        }

        if (request.offeredPrice() != null) {
            offer.setOfferedPrice(request.offeredPrice());
        }

        if (request.message() != null) {
            offer.setMessage(request.message());
        }

        Offer saved = offerRepository.save(offer);

        return mapper.toResponse(saved);
    }

    @Override
    public void deleteOffer(UUID id,
                            String ownerId) {

        Offer offer = offerRepository.findById(id)
                .orElseThrow(() ->
                        new OfferNotFoundException(id));

        if (!offer.getRequesterId().equals(ownerId)) {
            throw new RuntimeException("Access denied");
        }

        offer.setStatus(OfferStatus.WITHDRAWN);

        offerRepository.save(offer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfferResponse> getVendorOffers(String ownerId) {

        return offerRepository.findByVendorId(ownerId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OfferStatusResponse getOfferStatus(UUID offerId,
                                              String ownerId) {

        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() ->
                        new OfferNotFoundException(offerId));

        if (!offer.getVendorId().equals(ownerId)) {
            throw new RuntimeException("Access denied");
        }

        return new OfferStatusResponse(
                offer.getId(),
                offer.getStatus(),
                offer.getUpdatedAt()
        );
    }
}