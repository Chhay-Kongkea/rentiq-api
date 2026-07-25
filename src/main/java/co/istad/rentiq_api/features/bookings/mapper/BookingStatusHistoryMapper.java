package co.istad.rentiq_api.features.bookings.mapper;

import co.istad.rentiq_api.features.bookings.dto.response.BookingStatusHistoryResponse;
import co.istad.rentiq_api.features.bookings.entity.BookingStatusHistory;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BookingStatusHistoryMapper {

    BookingStatusHistoryResponse toResponse(BookingStatusHistory history);

}
