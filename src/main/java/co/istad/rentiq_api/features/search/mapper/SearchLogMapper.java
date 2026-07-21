package co.istad.rentiq_api.features.search.mapper;

import co.istad.rentiq_api.features.itemrequest.utils.GeographyUtils;
import co.istad.rentiq_api.features.search.dto.respone.SearchLogResponse;
import co.istad.rentiq_api.features.search.entity.SearchLog;
import org.locationtech.jts.geom.Point;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface SearchLogMapper {

    @Mapping(
            target = "latitude",
            expression = "java(latitude(searchLog.getLocation()))"
    )
    @Mapping(
            target = "longitude",
            expression = "java(longitude(searchLog.getLocation()))"
    )
    SearchLogResponse toResponse(SearchLog searchLog);

    default Double latitude(Point point) {
        return GeographyUtils.latitude(point);
    }

    default Double longitude(Point point) {
        return GeographyUtils.longitude(point);
    }
}