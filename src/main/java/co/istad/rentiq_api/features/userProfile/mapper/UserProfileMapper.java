package co.istad.rentiq_api.features.userProfile.mapper;


import co.istad.rentiq_api.features.userProfile.dto.response.AddressResponse;
import co.istad.rentiq_api.features.userProfile.dto.response.NotificationPreferencesResponse;
import co.istad.rentiq_api.features.userProfile.entity.NotificationPreference;
import co.istad.rentiq_api.features.userProfile.entity.UserAddress;
import co.istad.rentiq_api.features.userProfile.util.GeoUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(
        componentModel = "spring",
        imports = GeoUtils.class
)
public interface UserProfileMapper {

    @Mapping(
            target = "latitude",
            expression = "java(GeoUtils.latitude(address.getLocation()))"
    )
    @Mapping(
            target = "longitude",
            expression = "java(GeoUtils.longitude(address.getLocation()))"
    )
    AddressResponse toResponse(UserAddress address);@Mapping(target = "latitude", expression = "java(GeoUtils.latitude(address.getLocation()))")
    @Mapping(target = "longitude", expression = "java(GeoUtils.longitude(address.getLocation()))")
    AddressResponse toAddressResponse(UserAddress address);

    NotificationPreferencesResponse toPreferencesResponse(NotificationPreference prefs);
}


