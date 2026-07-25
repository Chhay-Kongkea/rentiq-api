package co.istad.rentiq_api.features.booking.mapper;

import co.istad.rentiq_api.features.booking.dto.response.*;
import co.istad.rentiq_api.features.booking.entity.Booking;
import co.istad.rentiq_api.features.booking.entity.BookingStatusHistory;
import co.istad.rentiq_api.features.userProfile.dto.response.AddressResponse;
import co.istad.rentiq_api.features.userProfile.util.GeoUtils;
import co.istad.rentiq_api.features.userProfile.entity.UserAddress;
import org.locationtech.jts.geom.Point;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    BookingResponse toResponse(Booking b);

    BookingStatusHistoryResponse toHistoryResponse(BookingStatusHistory h);



    @Mapping(target = "latitude", source = "location", qualifiedByName = "latitude")
    @Mapping(target = "longitude", source = "location", qualifiedByName = "longitude")
    AddressResponse toResponse(UserAddress address);

    @Named("latitude")
    default Double latitude(Point point) {
        return GeoUtils.latitude(point);
    }

    @Named("longitude")
    default Double longitude(Point point) {
        return GeoUtils.longitude(point);
    }


    @Mapping(target = "rentalStart",
            expression = "java(java.time.LocalDate.now())")
    ReceiptResponse toReceipt(Booking b);

    @Mapping(target = "issuedAt", expression = "java(java.time.Instant.now())")
    InvoiceResponse toInvoice(Booking b);
}