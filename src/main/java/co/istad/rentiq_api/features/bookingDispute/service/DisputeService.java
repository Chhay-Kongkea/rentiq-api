package co.istad.rentiq_api.features.bookingDispute.service;

import co.istad.rentiq_api.features.bookingDispute.dto.request.CreateDisputeRequest;
import co.istad.rentiq_api.features.bookingDispute.dto.request.ResolveDisputeRequest;
import co.istad.rentiq_api.features.bookingDispute.dto.request.UpdateDisputeRequest;


import co.istad.rentiq_api.features.booking.entity.Booking; // adjust package
import co.istad.rentiq_api.features.booking.repository.BookingRepository; // adjust package
import co.istad.rentiq_api.features.bookingDispute.dto.request.CreateDisputeRequest;
import co.istad.rentiq_api.features.bookingDispute.dto.request.ResolveDisputeRequest;
import co.istad.rentiq_api.features.bookingDispute.dto.request.UpdateDisputeRequest;
import co.istad.rentiq_api.features.bookingDispute.dto.response.DisputeResponse;
import co.istad.rentiq_api.features.bookingDispute.entity.BookingDispute;
import co.istad.rentiq_api.features.bookingDispute.exception.*;
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


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface DisputeService {

    DisputeResponse createDispute(String userId, UUID bookingId, CreateDisputeRequest request);

    List<DisputeResponse> listDisputesForBooking(String userId, UUID bookingId);

    DisputeResponse getDispute(String userId, UUID disputeId);

    DisputeResponse updateDispute(String userId, UUID disputeId, UpdateDisputeRequest request);

    Page<DisputeResponse> adminListDisputes(String status, Pageable pageable);

    DisputeResponse adminGetDispute(UUID disputeId);

    DisputeResponse adminResolveDispute(String adminId, UUID disputeId, ResolveDisputeRequest request);
}