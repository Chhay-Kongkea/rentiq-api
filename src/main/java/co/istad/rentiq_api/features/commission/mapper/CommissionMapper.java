package co.istad.rentiq_api.features.commission.mapper;

import co.istad.rentiq_api.features.category.Category;
import co.istad.rentiq_api.features.commission.dto.response.CommissionRateResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CommissionMapper {

    @Mapping(target = "categoryId", source = "id")
    @Mapping(target = "categoryName", source = "name")
    @Mapping(target = "categorySlug", source = "slug")
    CommissionRateResponse toRateResponse(Category category);
}
