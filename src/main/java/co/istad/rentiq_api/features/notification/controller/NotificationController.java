package co.istad.rentiq_api.features.notification.controller;

import co.istad.rentiq_api.features.item.dto.respone.PageResponse;
import co.istad.rentiq_api.features.notification.dto.response.NotificationResponse;
import co.istad.rentiq_api.features.notification.dto.response.NotificationUnreadCountResponse;
import co.istad.rentiq_api.features.notification.service.NotificationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('USER', 'VENDOR')")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public PageResponse<NotificationResponse> getNotifications(
            Authentication authentication,

            @RequestParam(defaultValue = "0")
            @Min(0)
            Integer pageNumber,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            Integer pageSize
    ) {
        return notificationService.getMyNotifications(
                authentication.getName(),
                pageNumber,
                pageSize
        );
    }

    @GetMapping("/{notificationId}")
    public NotificationResponse getNotification(
            @PathVariable UUID notificationId,
            Authentication authentication
    ) {
        return notificationService.getMyNotification(
                notificationId,
                authentication.getName()
        );
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markAsRead(
            @PathVariable UUID notificationId,
            Authentication authentication
    ) {
        return notificationService.markAsRead(
                notificationId,
                authentication.getName()
        );
    }

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead(
            Authentication authentication
    ) {
        notificationService.markAllAsRead(
                authentication.getName()
        );
    }

    @DeleteMapping("/{notificationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNotification(
            @PathVariable UUID notificationId,
            Authentication authentication
    ) {
        notificationService.deleteNotification(
                notificationId,
                authentication.getName()
        );
    }

    @GetMapping("/unread-count")
    public NotificationUnreadCountResponse getUnreadCount(
            Authentication authentication
    ) {
        return notificationService.getUnreadCount(
                authentication.getName()
        );
    }
}
