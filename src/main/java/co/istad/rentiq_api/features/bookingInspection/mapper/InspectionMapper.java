package co.istad.rentiq_api.features.bookingInspection.mapper;

import co.istad.rentiq_api.features.bookingInspection.dto.response.InspectionImageResponse;
import co.istad.rentiq_api.features.bookingInspection.dto.response.InspectionResponse;
import co.istad.rentiq_api.features.bookingInspection.entity.BookingInspection;
import co.istad.rentiq_api.features.bookingInspection.entity.InspectionImage;
import co.istad.rentiq_api.features.bookingInspection.repository.InspectionImageRepository;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class InspectionMapper {

    @Autowired
    protected InspectionImageRepository imageRepository;

    public InspectionResponse toResponse(BookingInspection inspection) {
        var images = imageRepository.findByInspectionIdOrderByCreatedAtAsc(inspection.getId())
                .stream()
                .map(this::toImageResponse)
                .toList();

        return toResponse(inspection, images);
    }

    // MapStruct generates this: maps entity fields directly, plus takes
    // the already-computed images list as a separate source parameter.
    protected abstract InspectionResponse toResponse(BookingInspection inspection, java.util.List<InspectionImageResponse> images);

    public abstract InspectionImageResponse toImageResponse(InspectionImage image);
}