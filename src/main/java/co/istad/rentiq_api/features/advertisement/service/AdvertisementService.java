package co.istad.rentiq_api.features.advertisement.service;

import co.istad.rentiq_api.features.advertisement.dto.request.CreateAdvertisementRequest;
import co.istad.rentiq_api.features.advertisement.dto.request.RejectAdvertisementRequest;
import co.istad.rentiq_api.features.advertisement.dto.request.UpdateAdvertisementRequest;
import co.istad.rentiq_api.features.advertisement.dto.response.AdvertisementResponse;
import co.istad.rentiq_api.features.advertisement.dto.response.PublicAdvertisementResponse;
import co.istad.rentiq_api.features.advertisement.enums.AdvertisementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface AdvertisementService {

    AdvertisementResponse create(CreateAdvertisementRequest request, String vendorId);

    AdvertisementResponse update(UUID id, UpdateAdvertisementRequest request, String vendorId);

    void cancel(UUID id, String vendorId);

    Page<AdvertisementResponse> getMyAdvertisements(String vendorId, AdvertisementStatus status, Pageable pageable);

    Page<PublicAdvertisementResponse> getPublicAdvertisements(UUID itemId, Pageable pageable);

    PublicAdvertisementResponse getPublicAdvertisement(UUID id);

    Page<AdvertisementResponse> adminList(
            AdvertisementStatus status, String vendorId, LocalDate from, LocalDate to, Pageable pageable);

    AdvertisementResponse adminApprove(UUID id, String adminId);

    AdvertisementResponse adminReject(UUID id, RejectAdvertisementRequest request, String adminId);

    AdvertisementResponse adminExpire(UUID id, String adminId);
}
