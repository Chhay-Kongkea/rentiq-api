package co.istad.rentiq_api.features.itemrequest.service;

import co.istad.rentiq_api.features.item.dto.respone.PageResponse;
import co.istad.rentiq_api.features.itemrequest.dto.request.CreateOfferRequest;
import co.istad.rentiq_api.features.itemrequest.dto.request.UpdateOfferRequest;
import co.istad.rentiq_api.features.itemrequest.dto.response.OfferResponse;
import co.istad.rentiq_api.features.itemrequest.dto.response.OfferStatusResponse;

import java.util.List;
import java.util.UUID;

public interface OfferService {

    OfferResponse createOffer(UUID requestId, CreateOfferRequest request, String authenticatedOwnerId);
    OfferResponse getOfferById(UUID offerId, String authenticatedOwnerId);

    List<OfferResponse> getRequestOffers(UUID requestId, String authenticatedCustomerId);
    OfferResponse updateOffer(UUID offerId, UpdateOfferRequest request, String authenticatedOwnerId);

    void withdrawOffer(UUID offerId, String authenticatedOwnerId);
    OfferResponse acceptOffer(UUID requestId, UUID offerId, String authenticatedCustomerId);
    OfferResponse rejectOffer(UUID requestId, UUID offerId, String authenticatedCustomerId);
    PageResponse<OfferResponse> getMyOffers(String authenticatedOwnerId, Integer pageNumber, Integer pageSize);
    OfferStatusResponse getMyOfferStatus(UUID offerId, String authenticatedOwnerId);
}