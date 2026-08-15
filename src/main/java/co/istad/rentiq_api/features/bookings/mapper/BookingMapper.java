package co.istad.rentiq_api.features.bookings.mapper;

import co.istad.rentiq_api.features.bookings.dto.response.BookingResponse;
import co.istad.rentiq_api.features.bookings.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BookingMapper {

    @Mapping(target = "itemId", source = "item.id")
    BookingResponse toResponse(Booking booking);

}
