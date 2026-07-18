package co.istad.rentiq_api.features.offers.mapper;

import co.istad.rentiq_api.features.offers.dto.request.CreateOfferRequest;
import co.istad.rentiq_api.features.offers.dto.response.OfferResponse;
import co.istad.rentiq_api.features.offers.entity.Offer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OfferMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "item", ignore = true)
    @Mapping(target = "requesterId", ignore = true)
    @Mapping(target = "vendorId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Offer toEntity(CreateOfferRequest request);

    @Mapping(target = "itemId", source = "item.id")
    OfferResponse toResponse(Offer offer);
}