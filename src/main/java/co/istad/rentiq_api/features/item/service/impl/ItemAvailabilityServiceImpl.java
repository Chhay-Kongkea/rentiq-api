package co.istad.rentiq_api.features.item.service.impl;


import co.istad.rentiq_api.features.bookings.entity.Booking;
import co.istad.rentiq_api.features.bookings.enums.BookingStatus;
import co.istad.rentiq_api.features.bookings.repository.BookingRepository;
import co.istad.rentiq_api.features.item.dto.request.CreateAvailabilityBlockRequest;
import co.istad.rentiq_api.features.item.dto.respone.AvailabilityBlockResponse;
import co.istad.rentiq_api.features.item.entity.Item;
import co.istad.rentiq_api.features.item.entity.ItemAvailabilityBlock;
import co.istad.rentiq_api.features.item.exception.AvailabilityBlockNotFoundException;
import co.istad.rentiq_api.features.item.exception.InvalidItemOperationException;
import co.istad.rentiq_api.features.item.exception.ItemAccessDeniedException;
import co.istad.rentiq_api.features.item.exception.ItemNotFoundException;
import co.istad.rentiq_api.features.item.repository.ItemAvailabilityBlockRepository;
import co.istad.rentiq_api.features.item.repository.ItemRepository;
import co.istad.rentiq_api.features.item.service.ItemAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemAvailabilityServiceImpl implements ItemAvailabilityService {

    private static final List<BookingStatus> BLOCKING_BOOKING_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.APPROVED, BookingStatus.RENTED);

    private final ItemRepository itemRepository;
    private final ItemAvailabilityBlockRepository blockRepository;
    private final BookingRepository bookingRepository;

    @Override
    public List<AvailabilityBlockResponse> getBlockedRanges(UUID itemId) {
        if (!itemRepository.findByIdAndDeletedFalse(itemId).isPresent()) {
            throw new ItemNotFoundException(itemId);
        }

        Stream<AvailabilityBlockResponse> manualBlocks = blockRepository
                .findByItem_IdOrderByStartDateAsc(itemId)
                .stream()
                .map(this::toResponse);

        Stream<AvailabilityBlockResponse> bookingBlocks = bookingRepository
                .findByItem_IdAndStatusInOrderByRentalStartAsc(itemId, BLOCKING_BOOKING_STATUSES)
                .stream()
                .map(this::toResponse);

        return Stream.concat(manualBlocks, bookingBlocks)
                .sorted(Comparator.comparing(AvailabilityBlockResponse::startDate))
                .toList();
    }

    @Override
    @Transactional
    public AvailabilityBlockResponse createBlock(UUID itemId, CreateAvailabilityBlockRequest request, String authenticatedUserId) {
        Item item = getOwnedItem(itemId, authenticatedUserId);

        if (request.endDate().isBefore(request.startDate())) {
            throw new InvalidItemOperationException("End date must not be before start date");
        }

        if (request.endDate().isBefore(LocalDate.now())) {
            throw new InvalidItemOperationException("Cannot block a date range entirely in the past");
        }

        if (blockRepository.existsOverlapping(itemId, request.startDate(), request.endDate())) {
            throw new InvalidItemOperationException("This item already has a block overlapping the given dates");
        }

        ItemAvailabilityBlock block = ItemAvailabilityBlock.builder()
                .item(item)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .reason(request.reason())
                .createdBy(authenticatedUserId)
                .build();

        ItemAvailabilityBlock saved = blockRepository.save(block);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteBlock(UUID itemId, UUID blockId, String authenticatedUserId) {
        getOwnedItem(itemId, authenticatedUserId);

        ItemAvailabilityBlock block = blockRepository
                .findByIdAndItem_Id(blockId, itemId)
                .orElseThrow(() -> new AvailabilityBlockNotFoundException(blockId));

        blockRepository.delete(block);
    }

    private Item getOwnedItem(UUID itemId, String authenticatedUserId) {
        Item item = itemRepository.findByIdAndDeletedFalse(itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        if (!item.getOwnerId().equals(authenticatedUserId)) {
            throw new ItemAccessDeniedException();
        }

        return item;
    }

    private AvailabilityBlockResponse toResponse(ItemAvailabilityBlock block) {
        return new AvailabilityBlockResponse(
                block.getId(),
                block.getItem().getId(),
                block.getStartDate(),
                block.getEndDate(),
                block.getReason(),
                "MANUAL",
                block.getCreatedAt()
        );
    }

    private AvailabilityBlockResponse toResponse(Booking booking) {
        return new AvailabilityBlockResponse(
                booking.getId(),
                booking.getItem().getId(),
                booking.getRentalStart(),
                booking.getRentalEnd(),
                null,
                "BOOKING",
                booking.getCreatedAt()
        );
    }
}
