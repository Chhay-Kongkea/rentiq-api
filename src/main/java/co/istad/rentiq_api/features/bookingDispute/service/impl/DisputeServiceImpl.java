package co.istad.rentiq_api.features.bookingDispute.service.impl;

import co.istad.rentiq_api.features.booking.entity.Booking;
import co.istad.rentiq_api.features.booking.repository.BookingRepository;
import co.istad.rentiq_api.features.bookingDispute.dto.request.CreateDisputeRequest;
import co.istad.rentiq_api.features.bookingDispute.dto.request.ResolveDisputeRequest;
import co.istad.rentiq_api.features.bookingDispute.dto.request.UpdateDisputeRequest;
import co.istad.rentiq_api.features.bookingDispute.dto.response.DisputeResponse;
import co.istad.rentiq_api.features.bookingDispute.entity.BookingDispute;
import co.istad.rentiq_api.features.bookingDispute.exception.DisputeAccessDeniedException;
import co.istad.rentiq_api.features.bookingDispute.exception.DisputeBookingNotFoundException;
import co.istad.rentiq_api.features.bookingDispute.exception.DisputeNotFoundException;
import co.istad.rentiq_api.features.bookingDispute.exception.DisputeNotOpenException;
import co.istad.rentiq_api.features.bookingDispute.mapper.DisputeMapper;
import co.istad.rentiq_api.features.bookingDispute.repository.BookingDisputeRepository;
import co.istad.rentiq_api.features.bookingDispute.service.DisputeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DisputeServiceImpl implements DisputeService {

    private final BookingDisputeRepository disputeRepository;
    private final BookingRepository bookingRepository;
    private final DisputeMapper disputeMapper;

    @Override
    @Transactional
    public DisputeResponse createDispute(String userId, UUID bookingId, CreateDisputeRequest request) {
        Booking booking = requireParticipantBooking(userId, bookingId);

        BookingDispute dispute = BookingDispute.builder()
                .bookingId(booking.getId())
                .openedBy(userId)
                .disputeType(request.disputeType())
                .description(request.description())
                .status("OPEN")
                .createdAt(Instant.now())
                .build();

        disputeRepository.save(dispute);
        return disputeMapper.toResponse(dispute);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisputeResponse> listDisputesForBooking(String userId, UUID bookingId) {
        requireParticipantBooking(userId, bookingId);
        return disputeRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .map(disputeMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DisputeResponse getDispute(String userId, UUID disputeId) {
        BookingDispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new DisputeNotFoundException(disputeId));
        requireParticipantBooking(userId, dispute.getBookingId());
        return disputeMapper.toResponse(dispute);
    }

    @Override
    @Transactional
    public DisputeResponse updateDispute(String userId, UUID disputeId, UpdateDisputeRequest request) {
        BookingDispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new DisputeNotFoundException(disputeId));

        if (!dispute.getOpenedBy().equals(userId)) {
            throw new DisputeAccessDeniedException("Only the person who opened this dispute can edit it");
        }
        if (!"OPEN".equals(dispute.getStatus())) {
            throw new DisputeNotOpenException(disputeId);
        }

        if (request.disputeType() != null) dispute.setDisputeType(request.disputeType());
        if (request.description() != null) dispute.setDescription(request.description());

        disputeRepository.save(dispute);
        return disputeMapper.toResponse(dispute);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DisputeResponse> adminListDisputes(String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return disputeRepository.findByStatus(status.toUpperCase(), pageable).map(disputeMapper::toResponse);
        }
        return disputeRepository.findAll(pageable).map(disputeMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public DisputeResponse adminGetDispute(UUID disputeId) {
        BookingDispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new DisputeNotFoundException(disputeId));
        return disputeMapper.toResponse(dispute);
    }

    @Override
    @Transactional
    public DisputeResponse adminResolveDispute(String adminId, UUID disputeId, ResolveDisputeRequest request) {
        BookingDispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new DisputeNotFoundException(disputeId));

        dispute.setStatus(request.status().toUpperCase());
        dispute.setResolvedBy(adminId);
        dispute.setResolvedAt(Instant.now());
        if (request.notes() != null && !request.notes().isBlank()) {
            String base = dispute.getDescription() == null ? "" : dispute.getDescription();
            dispute.setDescription(base + "\n\n[Admin note]: " + request.notes());
        }

        disputeRepository.save(dispute);
        return disputeMapper.toResponse(dispute);
    }

    private Booking requireParticipantBooking(String userId, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new DisputeBookingNotFoundException(bookingId));
        if (!booking.getCustomerId().equals(userId) && !booking.getOwnerId().equals(userId)) {
            throw new DisputeAccessDeniedException("You do not have access to this booking's disputes");
        }
        return booking;
    }
}
