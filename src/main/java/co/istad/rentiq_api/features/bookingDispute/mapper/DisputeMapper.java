package co.istad.rentiq_api.features.bookingDispute.mapper;

import co.istad.rentiq_api.features.bookingDispute.dto.response.DisputeResponse;
import co.istad.rentiq_api.features.bookingDispute.entity.BookingDispute;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DisputeMapper {

    DisputeResponse toResponse(BookingDispute d);
}