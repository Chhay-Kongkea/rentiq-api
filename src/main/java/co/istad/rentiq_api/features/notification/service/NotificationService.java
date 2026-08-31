package co.istad.rentiq_api.features.notification.service;

import co.istad.rentiq_api.features.item.dto.respone.PageResponse;
import co.istad.rentiq_api.features.notification.dto.request.BroadcastNotificationRequest;
import co.istad.rentiq_api.features.notification.dto.response.AdminNotificationResponse;
import co.istad.rentiq_api.features.notification.dto.response.BroadcastNotificationResponse;
import co.istad.rentiq_api.features.notification.dto.response.NotificationResponse;
import co.istad.rentiq_api.features.notification.dto.response.NotificationUnreadCountResponse;
import co.istad.rentiq_api.features.notification.enums.NotificationReferenceType;
import co.istad.rentiq_api.features.notification.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface NotificationService {

    /**
     * Internal, business-event notification for a single recipient. Not exposed as a public
     * API — callers are trusted business services, and recipientUserId must always come from
     * the affected domain entity (e.g. kyc.getUserId()), never from client-supplied input.
     */
    void notifyUser(
            String recipientUserId,
            NotificationType type,
            String title,
            String body,
            NotificationReferenceType referenceType,
            UUID referenceId
    );

    PageResponse<NotificationResponse> getMyNotifications(
            String authenticatedUserId,
            Integer pageNumber,
            Integer pageSize
    );

    NotificationResponse getMyNotification(
            UUID notificationId,
            String authenticatedUserId
    );

    NotificationResponse markAsRead(
            UUID notificationId,
            String authenticatedUserId
    );

    void markAllAsRead(
            String authenticatedUserId
    );

    void deleteNotification(
            UUID notificationId,
            String authenticatedUserId
    );

    NotificationUnreadCountResponse getUnreadCount(
            String authenticatedUserId
    );

    BroadcastNotificationResponse broadcast(
            BroadcastNotificationRequest request
    );

    /**
     * Operational history across all recipients — distinct from getMyNotifications, which is
     * scoped to the authenticated caller. Reuses the same Notification table; no second store.
     */
    Page<AdminNotificationResponse> adminListNotifications(
            String userId,
            NotificationType type,
            Boolean read,
            NotificationReferenceType referenceType,
            UUID referenceId,
            LocalDate createdFrom,
            LocalDate createdTo,
            Pageable pageable
    );

    AdminNotificationResponse adminGetNotification(UUID id);
}