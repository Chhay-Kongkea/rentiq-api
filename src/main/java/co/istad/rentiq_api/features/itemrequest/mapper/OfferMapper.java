package co.istad.rentiq_api.features.itemrequest.mapper;

import co.istad.rentiq_api.features.itemrequest.dto.response.OfferResponse;
import co.istad.rentiq_api.features.itemrequest.entity.Offer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OfferMapper {

    @Mapping(target = "requestId", source = "itemRequest.id")
    @Mapping(target = "itemId", source = "item.id")
    @Mapping(target = "itemTitle", source = "item.title")
    OfferResponse toResponse(Offer offer);
}