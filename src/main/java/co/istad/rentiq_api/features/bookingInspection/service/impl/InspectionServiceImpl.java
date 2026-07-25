package co.istad.rentiq_api.features.bookingInspection.service.impl;

import co.istad.rentiq_api.features.booking.entity.Booking; // adjust package
import co.istad.rentiq_api.features.booking.repository.BookingRepository; // adjust package
import co.istad.rentiq_api.features.bookingInspection.dto.request.AddInspectionImagesRequest;
import co.istad.rentiq_api.features.bookingInspection.dto.request.InspectionImageInput;
import co.istad.rentiq_api.features.bookingInspection.dto.request.UpsertInspectionRequest;
import co.istad.rentiq_api.features.bookingInspection.dto.response.InspectionImageResponse;
import co.istad.rentiq_api.features.bookingInspection.dto.response.InspectionResponse;
import co.istad.rentiq_api.features.bookingInspection.entity.BookingInspection;
import co.istad.rentiq_api.features.bookingInspection.entity.InspectionImage;
import co.istad.rentiq_api.features.bookingInspection.exception.*;
import co.istad.rentiq_api.features.bookingInspection.mapper.InspectionMapper;
import co.istad.rentiq_api.features.bookingInspection.repository.BookingInspectionRepository;
import co.istad.rentiq_api.features.bookingInspection.repository.InspectionImageRepository;
import co.istad.rentiq_api.features.bookingInspection.service.InspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InspectionServiceImpl implements InspectionService {

    private final BookingInspectionRepository inspectionRepository;
    private final InspectionImageRepository imageRepository;
    private final BookingRepository bookingRepository;
    private final InspectionMapper inspectionMapper;

    @Override
    @Transactional
    public InspectionResponse createInspection(String vendorId, UUID bookingId, UpsertInspectionRequest request) {
        Booking booking = requireOwnedBooking(vendorId, bookingId);
        if (inspectionRepository.existsByBookingId(booking.getId())) {
            throw new InspectionAlreadyExistsException(bookingId);
        }

        BookingInspection inspection = BookingInspection.builder()
                .bookingId(booking.getId())
                .checkInNotes(request.checkInNotes())
                .checkOutNotes(request.checkOutNotes())
                .createdAt(Instant.now())
                .build();

        inspectionRepository.save(inspection);
        return inspectionMapper.toResponse(inspection);
    }

    @Override
    @Transactional(readOnly = true)
    public InspectionResponse getInspection(String userId, UUID bookingId) {
        requireParticipantBooking(userId, bookingId);
        BookingInspection inspection = inspectionRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new InspectionNotFoundException(bookingId));
        return inspectionMapper.toResponse(inspection);
    }

    @Override
    @Transactional
    public InspectionResponse updateInspection(String vendorId, UUID bookingId, UpsertInspectionRequest request) {
        requireOwnedBooking(vendorId, bookingId);
        BookingInspection inspection = inspectionRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new InspectionNotFoundException(bookingId));

        if (request.checkInNotes() != null) inspection.setCheckInNotes(request.checkInNotes());
        if (request.checkOutNotes() != null) inspection.setCheckOutNotes(request.checkOutNotes());

        inspectionRepository.save(inspection);
        return inspectionMapper.toResponse(inspection);
    }

    @Override
    @Transactional
    public List<InspectionImageResponse> addImages(String vendorId, UUID bookingId, AddInspectionImagesRequest request) {
        requireOwnedBooking(vendorId, bookingId);
        BookingInspection inspection = inspectionRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new InspectionNotFoundException(bookingId));

        List<InspectionImage> images = request.images().stream()
                .map(input -> buildImage(inspection.getId(), input))
                .toList();
        imageRepository.saveAll(images);

        return images.stream().map(inspectionMapper::toImageResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InspectionImageResponse> listImages(String userId, UUID bookingId) {
        requireParticipantBooking(userId, bookingId);
        BookingInspection inspection = inspectionRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new InspectionNotFoundException(bookingId));
        return imageRepository.findByInspectionIdOrderByCreatedAtAsc(inspection.getId()).stream()
                .map(inspectionMapper::toImageResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteImage(String vendorId, UUID bookingId, UUID imageId) {
        requireOwnedBooking(vendorId, bookingId);
        BookingInspection inspection = inspectionRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new InspectionNotFoundException(bookingId));
        InspectionImage image = imageRepository.findByIdAndInspectionId(imageId, inspection.getId())
                .orElseThrow(() -> new InspectionImageNotFoundException(imageId));
        imageRepository.delete(image);
    }

    private InspectionImage buildImage(UUID inspectionId, InspectionImageInput input) {
        return InspectionImage.builder()
                .inspectionId(inspectionId)
                .imageName(input.imageName())
                .type(input.type())
                .createdAt(Instant.now())
                .build();
    }

    private Booking requireOwnedBooking(String vendorId, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new InspectionBookingNotFoundException(bookingId));
        if (!booking.getOwnerId().equals(vendorId)) {
            throw new InspectionAccessDeniedException("Only the item owner can manage this inspection");
        }
        return booking;
    }

    private void requireParticipantBooking(String userId, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new InspectionBookingNotFoundException(bookingId));
        if (!booking.getCustomerId().equals(userId) && !booking.getOwnerId().equals(userId)) {
            throw new InspectionAccessDeniedException("You do not have access to this booking's inspection");
        }
    }
}