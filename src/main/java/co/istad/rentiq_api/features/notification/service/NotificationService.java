package co.istad.rentiq_api.features.notification.service;

import co.istad.rentiq_api.features.item.dto.respone.PageResponse;
import co.istad.rentiq_api.features.notification.dto.request.BroadcastNotificationRequest;
import co.istad.rentiq_api.features.notification.dto.response.BroadcastNotificationResponse;
import co.istad.rentiq_api.features.notification.dto.response.NotificationResponse;
import co.istad.rentiq_api.features.notification.dto.response.NotificationUnreadCountResponse;

import java.util.UUID;

public interface NotificationService {

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
}