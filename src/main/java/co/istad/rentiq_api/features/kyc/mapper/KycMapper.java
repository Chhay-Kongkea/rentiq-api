package co.istad.rentiq_api.features.kyc.mapper;

import co.istad.rentiq_api.features.kyc.dto.response.AdminKycDetailResponse;
import co.istad.rentiq_api.features.kyc.dto.response.KycResponse;
import co.istad.rentiq_api.features.kyc.entity.UserKyc;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface KycMapper {

    KycResponse toResponse(UserKyc kyc);

    AdminKycDetailResponse toAdminDetailResponse(UserKyc kyc);
}