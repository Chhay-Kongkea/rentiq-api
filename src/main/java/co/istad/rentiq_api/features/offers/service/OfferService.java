package co.istad.rentiq_api.features.offers.service;



import co.istad.rentiq_api.features.offers.dto.request.UpdateOfferRequest;
import co.istad.rentiq_api.features.offers.dto.response.OfferResponse;
import co.istad.rentiq_api.features.offers.dto.request.CreateOfferRequest;
import co.istad.rentiq_api.features.offers.dto.response.OfferStatusResponse;

import java.util.List;
import java.util.UUID;

public interface OfferService {

    OfferResponse createOffer(UUID requestId,
                              CreateOfferRequest request,
                              String ownerId);

    OfferResponse getOffer(UUID id);

    OfferResponse updateOffer(UUID id,
                              UpdateOfferRequest request,
                              String ownerId);

    void deleteOffer(UUID id,
                     String ownerId);

    List<OfferResponse> getVendorOffers(String ownerId);

    OfferStatusResponse getOfferStatus(UUID offerId,
                                       String ownerId);

}