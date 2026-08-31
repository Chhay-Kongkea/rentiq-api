package co.istad.rentiq_api.features.promotion.service;

import co.istad.rentiq_api.features.promotion.dto.request.CreatePromotionRequest;
import co.istad.rentiq_api.features.promotion.dto.request.SuspendPromotionRequest;
import co.istad.rentiq_api.features.promotion.dto.response.PromotionResponse;
import co.istad.rentiq_api.features.promotion.dto.response.PromotionStatsResponse;
import co.istad.rentiq_api.features.promotion.enums.PromotionPackage;
import co.istad.rentiq_api.features.promotion.enums.PromotionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface PromotionService {

    PromotionResponse create(CreatePromotionRequest request, String vendorId);

    PromotionResponse getById(UUID id, String callerId, boolean isAdmin);

    Page<PromotionResponse> getMyPromotions(String vendorId, PromotionStatus status, Pageable pageable);

    PromotionResponse cancel(UUID id, String vendorId);

    PromotionStatsResponse getStats(UUID id, String callerId, boolean isAdmin);

    void recordImpression(UUID id);

    void recordClick(UUID id);

    Page<PromotionResponse> adminList(
            PromotionStatus status, String vendorId, UUID itemId, PromotionPackage packageType,
            LocalDate createdFrom, LocalDate createdTo, Pageable pageable);

    PromotionResponse adminSuspend(UUID id, SuspendPromotionRequest request, String adminId);
}
