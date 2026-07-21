package co.istad.rentiq_api.features.itemrequest.mapper;

import co.istad.rentiq_api.features.itemrequest.dto.request.CreateItemRequestRequest;
import co.istad.rentiq_api.features.itemrequest.dto.response.ItemRequestResponse;
import co.istad.rentiq_api.features.itemrequest.entity.ItemRequest;
import co.istad.rentiq_api.features.itemrequest.utils.GeographyUtils;
import org.locationtech.jts.geom.Point;
import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ItemRequestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerId", source = "authenticatedUserId")
    @Mapping(target = "location", expression = "java(toPoint(request.latitude(), request.longitude()))")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "offers", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ItemRequest toEntity(
            CreateItemRequestRequest request,
            String authenticatedUserId
    );

    @Mapping(target = "latitude", expression = "java(latitude(itemRequest.getLocation()))")
    @Mapping(target = "longitude", expression = "java(longitude(itemRequest.getLocation()))")
    @Mapping(target = "offerCount", expression = "java(itemRequest.getOffers() == null ? 0 : itemRequest.getOffers().size())")
    ItemRequestResponse toResponse(
            ItemRequest itemRequest
    );

    default Point toPoint(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }

        return GeographyUtils.createPoint(latitude, longitude);
    }

    default Double latitude(Point point) {
        return GeographyUtils.latitude(point);
    }

    default Double longitude(Point point) {
        return GeographyUtils.longitude(point);
    }
}