package co.istad.rentiq_api.features.notification.controller;

import co.istad.rentiq_api.features.notification.dto.request.BroadcastNotificationRequest;
import co.istad.rentiq_api.features.notification.dto.response.AdminNotificationResponse;
import co.istad.rentiq_api.features.notification.dto.response.BroadcastNotificationResponse;
import co.istad.rentiq_api.features.notification.enums.NotificationReferenceType;
import co.istad.rentiq_api.features.notification.enums.NotificationType;
import co.istad.rentiq_api.features.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Read-only operational history across all recipients, plus the existing broadcast action.
 * Not a normal user's inbox (see NotificationController for that) and not the central
 * AdminAuditLog (which answers "which admin changed what", not "who was notified").
 */
@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final NotificationService notificationService;

    @PostMapping("/broadcast")
    @ResponseStatus(HttpStatus.CREATED)
    public BroadcastNotificationResponse broadcast(
            @Valid
            @RequestBody
            BroadcastNotificationRequest request
    ) {
        return notificationService.broadcast(request);
    }

    @GetMapping
    public Page<AdminNotificationResponse> listNotifications(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) NotificationReferenceType referenceType,
            @RequestParam(required = false) UUID referenceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return notificationService.adminListNotifications(
                userId, type, read, referenceType, referenceId, createdFrom, createdTo, pageable);
    }

    @GetMapping("/{id}")
    public AdminNotificationResponse getNotification(@PathVariable UUID id) {
        return notificationService.adminGetNotification(id);
    }
}