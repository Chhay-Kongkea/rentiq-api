package co.istad.rentiq_api.features.bookingDispute.service.impl;

import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
import co.istad.rentiq_api.features.bookingDispute.dto.request.ResolveDisputeRequest;
import co.istad.rentiq_api.features.bookingDispute.entity.BookingDispute;
import co.istad.rentiq_api.features.bookingDispute.mapper.DisputeMapper;
import co.istad.rentiq_api.features.bookingDispute.repository.BookingDisputeRepository;
import co.istad.rentiq_api.features.bookings.entity.Booking;
import co.istad.rentiq_api.features.bookings.repository.BookingRepository;
import co.istad.rentiq_api.features.notification.enums.NotificationReferenceType;
import co.istad.rentiq_api.features.notification.enums.NotificationType;
import co.istad.rentiq_api.features.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisputeServiceImplTest {

    private static final String CUSTOMER_ID = "customer-1";
    private static final String OWNER_ID = "owner-1";

    @Mock private BookingDisputeRepository disputeRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private DisputeMapper disputeMapper;
    @Mock private AdminAuditService adminAuditService;
    @Mock private NotificationService notificationService;

    private DisputeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DisputeServiceImpl(disputeRepository, bookingRepository, disputeMapper, adminAuditService, notificationService);
        lenient().when(disputeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private BookingDispute openDispute(UUID id, UUID bookingId) {
        return BookingDispute.builder().id(id).bookingId(bookingId).openedBy(CUSTOMER_ID).status("OPEN").build();
    }

    @Test
    void adminResolveDispute_notifiesBothCustomerAndOwner_whenDifferentUsers() {
        UUID disputeId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(openDispute(disputeId, bookingId)));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(
                Booking.builder().id(bookingId).customerId(CUSTOMER_ID).ownerId(OWNER_ID).build()));

        service.adminResolveDispute("admin-1", disputeId, new ResolveDisputeRequest("RESOLVED", "Refund issued"));

        verify(adminAuditService).record(
                eq(AdminAuditAction.DISPUTE_RESOLVED), eq(AdminAuditTargetType.DISPUTE), eq(disputeId.toString()),
                any(), any(), eq("Refund issued"));
        verify(notificationService).notifyUser(
                eq(CUSTOMER_ID), eq(NotificationType.DISPUTE), any(), any(), eq(NotificationReferenceType.DISPUTE), eq(disputeId));
        verify(notificationService).notifyUser(
                eq(OWNER_ID), eq(NotificationType.DISPUTE), any(), any(), eq(NotificationReferenceType.DISPUTE), eq(disputeId));
        verify(notificationService, times(2)).notifyUser(any(), any(), any(), any(), any(), any());
    }

    @Test
    void adminResolveDispute_notifiesOnce_whenCustomerAndOwnerAreSameUser() {
        UUID disputeId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(openDispute(disputeId, bookingId)));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(
                Booking.builder().id(bookingId).customerId(CUSTOMER_ID).ownerId(CUSTOMER_ID).build()));

        service.adminResolveDispute("admin-1", disputeId, new ResolveDisputeRequest("RESOLVED", null));

        verify(notificationService, times(1)).notifyUser(
                eq(CUSTOMER_ID), eq(NotificationType.DISPUTE), any(), any(), eq(NotificationReferenceType.DISPUTE), eq(disputeId));
    }
}
