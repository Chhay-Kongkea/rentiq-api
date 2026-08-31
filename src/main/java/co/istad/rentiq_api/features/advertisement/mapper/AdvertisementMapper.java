package co.istad.rentiq_api.features.advertisement.mapper;

import co.istad.rentiq_api.features.advertisement.dto.response.AdvertisementResponse;
import co.istad.rentiq_api.features.advertisement.dto.response.PublicAdvertisementResponse;
import co.istad.rentiq_api.features.advertisement.entity.Advertisement;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AdvertisementMapper {

    AdvertisementResponse toResponse(Advertisement advertisement);

    PublicAdvertisementResponse toPublicResponse(Advertisement advertisement);
}
