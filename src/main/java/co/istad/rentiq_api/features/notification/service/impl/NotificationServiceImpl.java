package co.istad.rentiq_api.features.notification.service.impl;

import co.istad.rentiq_api.common.exception.NotFoundException;
import co.istad.rentiq_api.features.item.dto.respone.PageResponse;
import co.istad.rentiq_api.features.notification.dto.request.BroadcastNotificationRequest;
import co.istad.rentiq_api.features.notification.dto.response.BroadcastNotificationResponse;
import co.istad.rentiq_api.features.notification.dto.response.NotificationResponse;
import co.istad.rentiq_api.features.notification.dto.response.NotificationUnreadCountResponse;
import co.istad.rentiq_api.features.notification.entity.Notification;
import co.istad.rentiq_api.features.notification.mapper.NotificationMapper;
import co.istad.rentiq_api.features.notification.repository.NotificationRepository;
import co.istad.rentiq_api.features.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    public PageResponse<NotificationResponse> getMyNotifications(String authenticatedUserId, Integer pageNumber, Integer pageSize) {
        Pageable pageable = PageRequest.of(
                normalizePageNumber(pageNumber),
                normalizePageSize(pageSize),
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );

        Page<NotificationResponse> responsePage = notificationRepository
                        .findAllByUserIdOrderByCreatedAtDesc(
                                authenticatedUserId,
                                pageable
                        )
                        .map(notificationMapper::toResponse);

        return PageResponse.from(responsePage);
    }

    @Override
    public NotificationResponse getMyNotification(UUID notificationId, String authenticatedUserId) {
        Notification notification = getOwnedNotification(
                        notificationId,
                        authenticatedUserId
                );

        return notificationMapper.toResponse(notification);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, String authenticatedUserId) {
        Notification notification = getOwnedNotification(notificationId, authenticatedUserId);
        notification.markAsRead();
        Notification savedNotification = notificationRepository.save(notification);

        return notificationMapper.toResponse(savedNotification);
    }

    @Override
    @Transactional
    public void markAllAsRead(String authenticatedUserId) {
        notificationRepository.markAllAsRead(
                authenticatedUserId,
                OffsetDateTime.now()
        );
    }

    @Override
    @Transactional
    public void deleteNotification(UUID notificationId, String authenticatedUserId) {
        long deletedCount = notificationRepository.deleteByIdAndUserId(notificationId, authenticatedUserId);
        if (deletedCount == 0) {
            throw new NotFoundException("Notification", notificationId);
        }
    }

    @Override
    public NotificationUnreadCountResponse getUnreadCount(String authenticatedUserId) {
        long unreadCount = notificationRepository
                        .countByUserIdAndReadFalse(authenticatedUserId);

        return new NotificationUnreadCountResponse(unreadCount);
    }

    @Override
    @Transactional
    public BroadcastNotificationResponse broadcast(BroadcastNotificationRequest request) {
        List<Notification> notifications =
                request.userIds()
                        .stream()
                        .map(String::trim)
                        .filter(userId -> !userId.isBlank())
                        .distinct()
                        .map(userId ->
                                notificationMapper.toEntity(request, userId)
                        )
                        .toList();

        List<Notification> savedNotifications = notificationRepository.saveAll(notifications);

        return new BroadcastNotificationResponse(
                notifications.size(),
                savedNotifications.size()
        );
    }

    private Notification getOwnedNotification(UUID notificationId, String authenticatedUserId) {
        return notificationRepository
                .findByIdAndUserId(notificationId, authenticatedUserId)
                .orElseThrow(
                        () -> new NotFoundException("Notification", notificationId)
                );
    }

    private int normalizePageNumber(Integer pageNumber) {
        return pageNumber == null
                ? 0 : Math.max(pageNumber, 0);
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.max(1, Math.min(pageSize, MAX_PAGE_SIZE));
    }
}