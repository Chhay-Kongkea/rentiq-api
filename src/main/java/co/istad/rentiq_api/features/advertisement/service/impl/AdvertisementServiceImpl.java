package co.istad.rentiq_api.features.advertisement.service.impl;

import co.istad.rentiq_api.common.exception.ForbiddenException;
import co.istad.rentiq_api.common.exception.InvalidOperationException;
import co.istad.rentiq_api.common.exception.InvalidStateException;
import co.istad.rentiq_api.common.exception.NotFoundException;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
import co.istad.rentiq_api.features.advertisement.dto.request.CreateAdvertisementRequest;
import co.istad.rentiq_api.features.advertisement.dto.request.RejectAdvertisementRequest;
import co.istad.rentiq_api.features.advertisement.dto.request.UpdateAdvertisementRequest;
import co.istad.rentiq_api.features.advertisement.dto.response.AdvertisementResponse;
import co.istad.rentiq_api.features.advertisement.dto.response.PublicAdvertisementResponse;
import co.istad.rentiq_api.features.advertisement.entity.Advertisement;
import co.istad.rentiq_api.features.advertisement.enums.AdvertisementPackage;
import co.istad.rentiq_api.features.advertisement.enums.AdvertisementStatus;
import co.istad.rentiq_api.features.advertisement.mapper.AdvertisementMapper;
import co.istad.rentiq_api.features.advertisement.repository.AdvertisementRepository;
import co.istad.rentiq_api.features.advertisement.service.AdvertisementService;
import co.istad.rentiq_api.features.advertisement.specification.AdvertisementSpecification;
import co.istad.rentiq_api.features.item.entity.Item;
import co.istad.rentiq_api.features.item.enums.ItemApprovalStatus;
import co.istad.rentiq_api.features.item.enums.ItemStatus;
import co.istad.rentiq_api.features.item.exception.ItemNotFoundException;
import co.istad.rentiq_api.features.item.repository.ItemRepository;
import co.istad.rentiq_api.features.notification.enums.NotificationReferenceType;
import co.istad.rentiq_api.features.notification.enums.NotificationType;
import co.istad.rentiq_api.features.notification.service.NotificationService;
import co.istad.rentiq_api.features.platformPricing.service.PlatformPricingService;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdvertisementServiceImpl implements AdvertisementService {

    private static final ZoneOffset REPORTING_ZONE = ZoneOffset.UTC;
    private static final List<AdvertisementStatus> PUBLICLY_VISIBLE_STATUSES =
            List.of(AdvertisementStatus.APPROVED, AdvertisementStatus.ACTIVE);
    private static final int MAX_START_DAYS_AHEAD = 30;

    private final AdvertisementRepository advertisementRepository;
    private final ItemRepository itemRepository;
    private final AdvertisementMapper advertisementMapper;
    private final AdminAuditService adminAuditService;
    private final NotificationService notificationService;
    private final WalletService walletService;
    private final PlatformPricingService platformPricingService;

    @Override
    @Transactional
    public AdvertisementResponse create(CreateAdvertisementRequest request, String vendorId) {
        Item item = requireOwnedEligibleItem(request.itemId(), vendorId);
        validateStartAt(request.startAt());

        OffsetDateTime now = OffsetDateTime.now(REPORTING_ZONE);
        Quote quote = quoteFor(request.packageType(), vendorId);

        Advertisement advertisement = Advertisement.builder()
                .vendorId(vendorId)
                .itemId(item.getId())
                .title(request.title())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .packageType(request.packageType())
                .durationDays(request.packageType().getDurationDays())
                .quotedPrice(quote.price())
                .quotedCurrency(quote.currency())
                .quotedAt(now)
                .status(AdvertisementStatus.PENDING)
                .startAt(request.startAt())
                .endAt(request.startAt().plusDays(request.packageType().getDurationDays()))
                .build();

        return advertisementMapper.toResponse(advertisementRepository.save(advertisement));
    }

    @Override
    @Transactional
    public AdvertisementResponse update(UUID id, UpdateAdvertisementRequest request, String vendorId) {
        Advertisement advertisement = requireOwned(id, vendorId);
        validateStartAt(request.startAt());

        boolean isResubmission = advertisement.getStatus() == AdvertisementStatus.REJECTED;
        // A package change materially affects pricing; a content-only edit (title/description/
        // image/startAt) does not — packages are fixed-duration/fixed-price, so startAt alone
        // never changes what's owed. Re-quote only when the package actually changes, or on a
        // REJECTED resubmission (which is, by definition, a brand-new submission).
        boolean packageChanged = advertisement.getPackageType() != request.packageType();

        if (isResubmission) {
            // Resubmission: same pattern as KYC resubmission — clears the prior review.
            // A rejected advertisement was never charged, so price/currency stay unset —
            // never retain a stale quoted price as if payment had occurred.
            advertisement.setStatus(AdvertisementStatus.PENDING);
            advertisement.setRejectionReason(null);
            advertisement.setReviewedBy(null);
            advertisement.setReviewedAt(null);
            advertisement.setPrice(null);
            advertisement.setCurrency(null);
        } else if (advertisement.getStatus() != AdvertisementStatus.PENDING) {
            throw new InvalidStateException(
                    "Advertisement", advertisement.getStatus(),
                    "Only a pending or rejected advertisement can be edited");
        }

        if (isResubmission || packageChanged) {
            OffsetDateTime now = OffsetDateTime.now(REPORTING_ZONE);
            Quote quote = quoteFor(request.packageType(), vendorId);
            advertisement.setQuotedPrice(quote.price());
            advertisement.setQuotedCurrency(quote.currency());
            advertisement.setQuotedAt(now);
        }

        advertisement.setTitle(request.title());
        advertisement.setDescription(request.description());
        advertisement.setImageUrl(request.imageUrl());
        advertisement.setPackageType(request.packageType());
        advertisement.setDurationDays(request.packageType().getDurationDays());
        advertisement.setStartAt(request.startAt());
        advertisement.setEndAt(request.startAt().plusDays(request.packageType().getDurationDays()));

        return advertisementMapper.toResponse(advertisementRepository.save(advertisement));
    }

    @Override
    @Transactional
    public void cancel(UUID id, String vendorId) {
        Advertisement advertisement = requireOwned(id, vendorId);

        if (advertisement.getStatus() != AdvertisementStatus.PENDING
                && advertisement.getStatus() != AdvertisementStatus.APPROVED
                && advertisement.getStatus() != AdvertisementStatus.ACTIVE) {
            throw new InvalidStateException(
                    "Advertisement", advertisement.getStatus(),
                    "Only a pending, approved, or active advertisement can be cancelled");
        }

        advertisement.setStatus(AdvertisementStatus.CANCELLED);
        advertisementRepository.save(advertisement);
    }

    @Override
    public Page<AdvertisementResponse> getMyAdvertisements(String vendorId, AdvertisementStatus status, Pageable pageable) {
        Page<Advertisement> page = status != null
                ? advertisementRepository.findByVendorIdAndStatus(vendorId, status, pageable)
                : advertisementRepository.findByVendorId(vendorId, pageable);
        return page.map(advertisementMapper::toResponse);
    }

    @Override
    public Page<PublicAdvertisementResponse> getPublicAdvertisements(UUID itemId, Pageable pageable) {
        return advertisementRepository
                .findPubliclyVisible(PUBLICLY_VISIBLE_STATUSES, OffsetDateTime.now(REPORTING_ZONE), itemId, pageable)
                .map(advertisementMapper::toPublicResponse);
    }

    @Override
    public PublicAdvertisementResponse getPublicAdvertisement(UUID id) {
        Advertisement advertisement = advertisementRepository
                .findPubliclyVisibleById(id, PUBLICLY_VISIBLE_STATUSES, OffsetDateTime.now(REPORTING_ZONE))
                .orElseThrow(() -> new NotFoundException("Advertisement", id));
        return advertisementMapper.toPublicResponse(advertisement);
    }

    @Override
    public Page<AdvertisementResponse> adminList(
            AdvertisementStatus status, String vendorId, LocalDate from, LocalDate to, Pageable pageable) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new InvalidOperationException("from date must not be after to date");
        }

        OffsetDateTime fromInclusive = from == null ? null : from.atStartOfDay(REPORTING_ZONE).toOffsetDateTime();
        OffsetDateTime toExclusive = to == null ? null : to.plusDays(1).atStartOfDay(REPORTING_ZONE).toOffsetDateTime();

        return advertisementRepository
                .findAll(AdvertisementSpecification.adminFilter(status, vendorId, fromInclusive, toExclusive), pageable)
                .map(advertisementMapper::toResponse);
    }

    @Override
    @Transactional
    public AdvertisementResponse adminApprove(UUID id, String adminId) {
        Advertisement advertisement = advertisementRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Advertisement", id));

        if (advertisement.getStatus() != AdvertisementStatus.PENDING) {
            throw new InvalidStateException(
                    "Advertisement", advertisement.getStatus(), "Only a pending advertisement can be approved");
        }

        requireStillEligibleItem(advertisement.getItemId());

        OffsetDateTime now = OffsetDateTime.now(REPORTING_ZONE);
        if (!advertisement.getEndAt().isAfter(now)) {
            throw new InvalidOperationException(
                    "Advertisement", "Cannot approve an advertisement whose end time has already passed");
        }

        AdvertisementStatus newStatus = advertisement.getStartAt().isAfter(now)
                ? AdvertisementStatus.APPROVED
                : AdvertisementStatus.ACTIVE;

        if (advertisement.getQuotedPrice() == null || advertisement.getQuotedCurrency() == null) {
            throw new InvalidOperationException("Advertisement", "This advertisement has no price quote to approve");
        }

        // Charge the FROZEN quote from submission/resubmission time — never re-resolve the
        // current setting here. If Admin changed the price after this advertisement was
        // submitted, this vendor is unaffected; only advertisements submitted after the change
        // get the new price. Debit BEFORE mutating the advertisement: if this throws
        // (insufficient balance, wallet currency no longer matches the quote), nothing below
        // runs and the whole transaction rolls back — the advertisement stays PENDING untouched.
        WalletResponse wallet = walletService.getWallet(advertisement.getVendorId());
        if (!wallet.currency().equals(advertisement.getQuotedCurrency())) {
            throw new InvalidOperationException(
                    "Advertisement",
                    "Wallet currency (%s) no longer matches the quoted currency (%s) — cannot approve without re-quoting"
                            .formatted(wallet.currency(), advertisement.getQuotedCurrency()));
        }

        walletService.chargeAdvertisement(
                advertisement.getId(), advertisement.getVendorId(),
                advertisement.getQuotedPrice(), advertisement.getQuotedCurrency());

        BigDecimal price = advertisement.getQuotedPrice();
        advertisement.setPrice(price);
        advertisement.setCurrency(advertisement.getQuotedCurrency());
        advertisement.setStatus(newStatus);
        advertisement.setReviewedBy(adminId);
        advertisement.setReviewedAt(now);
        advertisement.setRejectionReason(null);
        advertisementRepository.save(advertisement);

        adminAuditService.record(
                AdminAuditAction.ADVERTISEMENT_APPROVED,
                AdminAuditTargetType.ADVERTISEMENT,
                id.toString(),
                Map.of("status", "PENDING"),
                Map.of("status", newStatus.name(), "packageType", advertisement.getPackageType().name(),
                        "price", price, "currency", wallet.currency()),
                null);

        notificationService.notifyUser(
                advertisement.getVendorId(),
                NotificationType.ADVERTISEMENT,
                "Ad approved",
                "Your advertisement has been approved.",
                NotificationReferenceType.ADVERTISEMENT,
                advertisement.getId());

        return advertisementMapper.toResponse(advertisement);
    }

    @Override
    @Transactional
    public AdvertisementResponse adminReject(UUID id, RejectAdvertisementRequest request, String adminId) {
        Advertisement advertisement = advertisementRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Advertisement", id));

        if (advertisement.getStatus() != AdvertisementStatus.PENDING) {
            throw new InvalidStateException(
                    "Advertisement", advertisement.getStatus(), "Only a pending advertisement can be rejected");
        }

        advertisement.setStatus(AdvertisementStatus.REJECTED);
        advertisement.setRejectionReason(request.reason());
        advertisement.setReviewedBy(adminId);
        advertisement.setReviewedAt(OffsetDateTime.now(REPORTING_ZONE));
        advertisementRepository.save(advertisement);

        adminAuditService.record(
                AdminAuditAction.ADVERTISEMENT_REJECTED,
                AdminAuditTargetType.ADVERTISEMENT,
                id.toString(),
                Map.of("status", "PENDING"),
                Map.of("status", "REJECTED"),
                request.reason());

        notificationService.notifyUser(
                advertisement.getVendorId(),
                NotificationType.ADVERTISEMENT,
                "Ad rejected",
                "Your advertisement was not approved. Please review the reason and update your submission.",
                NotificationReferenceType.ADVERTISEMENT,
                advertisement.getId());

        return advertisementMapper.toResponse(advertisement);
    }

    @Override
    @Transactional
    public AdvertisementResponse adminExpire(UUID id, String adminId) {
        Advertisement advertisement = advertisementRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Advertisement", id));

        if (advertisement.getStatus() != AdvertisementStatus.APPROVED
                && advertisement.getStatus() != AdvertisementStatus.ACTIVE) {
            throw new InvalidStateException(
                    "Advertisement", advertisement.getStatus(),
                    "Only an approved or active advertisement can be expired");
        }

        String previousStatus = advertisement.getStatus().name();
        advertisement.setStatus(AdvertisementStatus.EXPIRED);
        advertisementRepository.save(advertisement);

        adminAuditService.record(
                AdminAuditAction.ADVERTISEMENT_EXPIRED,
                AdminAuditTargetType.ADVERTISEMENT,
                id.toString(),
                Map.of("status", previousStatus),
                Map.of("status", "EXPIRED"),
                null);

        return advertisementMapper.toResponse(advertisement);
    }

    private Item requireOwnedEligibleItem(UUID itemId, String vendorId) {
        Item item = itemRepository.findByIdAndDeletedFalse(itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        if (!item.getOwnerId().equals(vendorId)) {
            throw new ForbiddenException("Item", "You can only advertise items you own");
        }

        if (item.getApprovalStatus() != ItemApprovalStatus.APPROVED || item.getStatus() != ItemStatus.ACTIVE) {
            throw new InvalidOperationException(
                    "Advertisement", "Only an approved, active listing can be advertised");
        }

        return item;
    }

    private Advertisement requireOwned(UUID id, String vendorId) {
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Advertisement", id));

        if (!advertisement.getVendorId().equals(vendorId)) {
            throw new ForbiddenException("Advertisement", "You can only manage your own advertisements");
        }

        return advertisement;
    }

    private void requireStillEligibleItem(UUID itemId) {
        Item item = itemRepository.findByIdAndDeletedFalse(itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        if (item.getApprovalStatus() != ItemApprovalStatus.APPROVED || item.getStatus() != ItemStatus.ACTIVE) {
            throw new InvalidOperationException(
                    "Advertisement", "Item is no longer an approved, active listing and cannot be approved for advertising");
        }
    }

    private void validateStartAt(OffsetDateTime startAt) {
        OffsetDateTime now = OffsetDateTime.now(REPORTING_ZONE);
        if (startAt.isBefore(now)) {
            throw new InvalidOperationException("Advertisement", "startAt must not be in the past");
        }
        if (startAt.isAfter(now.plusDays(MAX_START_DAYS_AHEAD))) {
            throw new InvalidOperationException(
                    "Advertisement", "startAt must not be more than " + MAX_START_DAYS_AHEAD + " days in the future");
        }
    }

    /**
     * Resolves the vendor's wallet currency and the current effective price for the given
     * package in that currency — the quote frozen onto the advertisement at
     * submission/resubmission time. Does NOT check wallet balance: the vendor may top up any
     * time before Admin approval, so only currency/pricing resolution needs to succeed here.
     */
    private Quote quoteFor(AdvertisementPackage packageType, String vendorId) {
        WalletResponse wallet = walletService.getWallet(vendorId);
        BigDecimal price = platformPricingService.getAdvertisementPrice(packageType, wallet.currency())
                .orElseThrow(() -> new InvalidOperationException(
                        "Advertisement", "Unsupported wallet currency for advertisement pricing: " + wallet.currency()));
        return new Quote(price, wallet.currency());
    }

    private record Quote(BigDecimal price, String currency) {
    }
}
