package co.istad.rentiq_api.features.promotion.service.impl;

import co.istad.rentiq_api.common.exception.ForbiddenException;
import co.istad.rentiq_api.common.exception.InvalidOperationException;
import co.istad.rentiq_api.common.exception.InvalidStateException;
import co.istad.rentiq_api.common.exception.NotFoundException;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
import co.istad.rentiq_api.features.item.entity.Item;
import co.istad.rentiq_api.features.item.enums.ItemApprovalStatus;
import co.istad.rentiq_api.features.item.enums.ItemStatus;
import co.istad.rentiq_api.features.item.exception.ItemNotFoundException;
import co.istad.rentiq_api.features.item.repository.ItemRepository;
import co.istad.rentiq_api.features.notification.enums.NotificationReferenceType;
import co.istad.rentiq_api.features.notification.enums.NotificationType;
import co.istad.rentiq_api.features.notification.service.NotificationService;
import co.istad.rentiq_api.features.platformPricing.service.PlatformPricingService;
import co.istad.rentiq_api.features.promotion.dto.request.CreatePromotionRequest;
import co.istad.rentiq_api.features.promotion.dto.request.SuspendPromotionRequest;
import co.istad.rentiq_api.features.promotion.dto.response.PromotionResponse;
import co.istad.rentiq_api.features.promotion.dto.response.PromotionStatsResponse;
import co.istad.rentiq_api.features.promotion.entity.Promotion;
import co.istad.rentiq_api.features.promotion.enums.PromotionPackage;
import co.istad.rentiq_api.features.promotion.enums.PromotionStatus;
import co.istad.rentiq_api.features.promotion.mapper.PromotionMapper;
import co.istad.rentiq_api.features.promotion.repository.PromotionRepository;
import co.istad.rentiq_api.features.promotion.service.PromotionService;
import co.istad.rentiq_api.features.promotion.specification.PromotionSpecification;
import co.istad.rentiq_api.features.wallet.dto.response.WalletResponse;
import co.istad.rentiq_api.features.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromotionServiceImpl implements PromotionService {

    private static final ZoneOffset REPORTING_ZONE = ZoneOffset.UTC;

    private final PromotionRepository promotionRepository;
    private final ItemRepository itemRepository;
    private final WalletService walletService;
    private final PromotionMapper promotionMapper;
    private final AdminAuditService adminAuditService;
    private final NotificationService notificationService;
    private final PlatformPricingService platformPricingService;

    @Override
    @Transactional
    public PromotionResponse create(CreatePromotionRequest request, String vendorId) {
        // Lock the Item row first — every concurrent purchase attempt for the same item
        // serializes here, which is what makes the "no active promotion" check below race-free.
        Item item = itemRepository.findByIdForUpdate(request.itemId())
                .orElseThrow(() -> new ItemNotFoundException(request.itemId()));

        if (item.isDeleted()) {
            throw new ItemNotFoundException(request.itemId());
        }
        if (!item.getOwnerId().equals(vendorId)) {
            throw new ForbiddenException("Item", "You can only promote items you own");
        }
        if (item.getApprovalStatus() != ItemApprovalStatus.APPROVED || item.getStatus() != ItemStatus.ACTIVE) {
            throw new InvalidOperationException("Promotion", "Only an approved, active listing can be promoted");
        }

        OffsetDateTime now = OffsetDateTime.now(REPORTING_ZONE);
        if (promotionRepository.existsEffectiveActiveForItem(item.getId(), now)) {
            throw new InvalidStateException("Promotion", "ACTIVE", "This item already has an active promotion");
        }

        WalletResponse wallet = walletService.getWallet(vendorId);
        BigDecimal price = platformPricingService.getPromotionPrice(request.packageType(), wallet.currency())
                .orElseThrow(() -> new InvalidOperationException(
                        "Promotion", "Unsupported wallet currency for promotion pricing: " + wallet.currency()));

        Promotion promotion = Promotion.builder()
                .vendorId(vendorId)
                .itemId(item.getId())
                .packageType(request.packageType())
                .durationDays(request.packageType().getDurationDays())
                .price(price)
                .currency(wallet.currency())
                .status(PromotionStatus.ACTIVE)
                .startAt(now)
                .endAt(now.plusDays(request.packageType().getDurationDays()))
                .build();

        Promotion saved = promotionRepository.save(promotion);

        // If this throws (insufficient balance / currency mismatch), the whole transaction —
        // including the Promotion insert above — rolls back. Nothing is left half-purchased.
        walletService.chargePromotion(saved.getId(), vendorId, price, wallet.currency());

        return promotionMapper.toResponse(saved);
    }

    @Override
    public PromotionResponse getById(UUID id, String callerId, boolean isAdmin) {
        return promotionMapper.toResponse(requireViewable(id, callerId, isAdmin));
    }

    @Override
    public Page<PromotionResponse> getMyPromotions(String vendorId, PromotionStatus status, Pageable pageable) {
        return promotionRepository
                .findAll(
                        PromotionSpecification.adminFilter(
                                status, vendorId, null, null, null, null, OffsetDateTime.now(REPORTING_ZONE)),
                        pageable)
                .map(promotionMapper::toResponse);
    }

    @Override
    @Transactional
    public PromotionResponse cancel(UUID id, String vendorId) {
        Promotion promotion = promotionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Promotion", id));

        if (!promotion.getVendorId().equals(vendorId)) {
            throw new ForbiddenException("Promotion", "You can only manage your own promotions");
        }
        if (!isEffectivelyActive(promotion, OffsetDateTime.now(REPORTING_ZONE))) {
            throw new InvalidStateException(
                    "Promotion", promotionMapper.effectiveStatus(promotion), "Only an active promotion can be cancelled");
        }

        // No refund: remaining time on a cancelled promotion is simply forfeited.
        promotion.setStatus(PromotionStatus.CANCELLED);
        promotion.setCancelledAt(OffsetDateTime.now(REPORTING_ZONE));

        return promotionMapper.toResponse(promotionRepository.save(promotion));
    }

    @Override
    public PromotionStatsResponse getStats(UUID id, String callerId, boolean isAdmin) {
        return promotionMapper.toStatsResponse(requireViewable(id, callerId, isAdmin));
    }

    @Override
    @Transactional
    public void recordImpression(UUID id) {
        requireExists(id);
        // Silently a no-op if not effectively active — see PromotionRepository for why this is
        // safe from lost-update races (a single conditional UPDATE, not load/increment/save).
        promotionRepository.incrementImpressionIfActive(id, OffsetDateTime.now(REPORTING_ZONE));
    }

    @Override
    @Transactional
    public void recordClick(UUID id) {
        requireExists(id);
        promotionRepository.incrementClickIfActive(id, OffsetDateTime.now(REPORTING_ZONE));
    }

    @Override
    public Page<PromotionResponse> adminList(
            PromotionStatus status, String vendorId, UUID itemId, PromotionPackage packageType,
            LocalDate createdFrom, LocalDate createdTo, Pageable pageable) {
        if (createdFrom != null && createdTo != null && createdTo.isBefore(createdFrom)) {
            throw new InvalidOperationException("createdFrom date must not be after createdTo date");
        }

        OffsetDateTime fromInclusive = createdFrom == null
                ? null : createdFrom.atStartOfDay(REPORTING_ZONE).toOffsetDateTime();
        OffsetDateTime toExclusive = createdTo == null
                ? null : createdTo.plusDays(1).atStartOfDay(REPORTING_ZONE).toOffsetDateTime();

        return promotionRepository
                .findAll(
                        PromotionSpecification.adminFilter(
                                status, vendorId, itemId, packageType, fromInclusive, toExclusive,
                                OffsetDateTime.now(REPORTING_ZONE)),
                        pageable)
                .map(promotionMapper::toResponse);
    }

    @Override
    @Transactional
    public PromotionResponse adminSuspend(UUID id, SuspendPromotionRequest request, String adminId) {
        if (request.status() != PromotionStatus.SUSPENDED) {
            throw new InvalidOperationException("Promotion", "This endpoint can only set status to SUSPENDED");
        }

        Promotion promotion = promotionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Promotion", id));

        if (!isEffectivelyActive(promotion, OffsetDateTime.now(REPORTING_ZONE))) {
            throw new InvalidStateException(
                    "Promotion", promotionMapper.effectiveStatus(promotion), "Only an active promotion can be suspended");
        }

        promotion.setStatus(PromotionStatus.SUSPENDED);
        promotion.setSuspendedBy(adminId);
        promotion.setSuspendedAt(OffsetDateTime.now(REPORTING_ZONE));
        promotion.setSuspensionReason(request.reason());
        promotionRepository.save(promotion);

        adminAuditService.record(
                AdminAuditAction.PROMOTION_SUSPENDED,
                AdminAuditTargetType.PROMOTION,
                id.toString(),
                Map.of("status", "ACTIVE"),
                Map.of("status", "SUSPENDED"),
                request.reason());

        notificationService.notifyUser(
                promotion.getVendorId(),
                NotificationType.PROMOTION,
                "Promotion suspended",
                "Your item promotion has been suspended by an administrator.",
                NotificationReferenceType.PROMOTION,
                promotion.getId());

        return promotionMapper.toResponse(promotion);
    }

    private Promotion requireViewable(UUID id, String callerId, boolean isAdmin) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promotion", id));

        if (!isAdmin && !promotion.getVendorId().equals(callerId)) {
            throw new ForbiddenException("Promotion", "You can only view your own promotions");
        }

        return promotion;
    }

    private void requireExists(UUID id) {
        if (!promotionRepository.existsById(id)) {
            throw new NotFoundException("Promotion", id);
        }
    }

    private boolean isEffectivelyActive(Promotion promotion, OffsetDateTime now) {
        return promotion.getStatus() == PromotionStatus.ACTIVE
                && !promotion.getStartAt().isAfter(now)
                && promotion.getEndAt().isAfter(now);
    }
}
