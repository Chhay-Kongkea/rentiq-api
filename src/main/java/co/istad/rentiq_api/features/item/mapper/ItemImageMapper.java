package co.istad.rentiq_api.features.item.mapper;

import co.istad.rentiq_api.features.item.dto.respone.ItemImageResponse;
import co.istad.rentiq_api.features.item.entity.ItemImage;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ItemImageMapper {
    ItemImageResponse toResponse(ItemImage image);
}
