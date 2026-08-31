package co.istad.rentiq_api.features.notification.service.impl;

import co.istad.rentiq_api.common.exception.InvalidOperationException;
import co.istad.rentiq_api.common.exception.NotFoundException;
import co.istad.rentiq_api.common.exception.InvalidStateException;
import co.istad.rentiq_api.common.config.props.KeycloakAdminClientProps;
import co.istad.rentiq_api.features.auth.RoleEnum;
import co.istad.rentiq_api.features.auth.exception.KeycloakOperationException;
import co.istad.rentiq_api.features.item.dto.respone.PageResponse;
import co.istad.rentiq_api.features.notification.dto.request.BroadcastNotificationRequest;
import co.istad.rentiq_api.features.notification.dto.response.AdminNotificationResponse;
import co.istad.rentiq_api.features.notification.dto.response.BroadcastNotificationResponse;
import co.istad.rentiq_api.features.notification.dto.response.NotificationResponse;
import co.istad.rentiq_api.features.notification.dto.response.NotificationUnreadCountResponse;
import co.istad.rentiq_api.features.notification.entity.Notification;
import co.istad.rentiq_api.features.notification.enums.NotificationReferenceType;
import co.istad.rentiq_api.features.notification.enums.NotificationType;
import co.istad.rentiq_api.features.notification.mapper.NotificationMapper;
import co.istad.rentiq_api.features.notification.repository.NotificationRepository;
import co.istad.rentiq_api.features.notification.service.NotificationService;
import co.istad.rentiq_api.features.notification.specification.NotificationSpecification;
import co.istad.rentiq_api.features.notification.NotificationPersistenceMapper;
import co.istad.rentiq_api.features.notification.enums.BroadcastAudienceType;
import co.istad.rentiq_api.features.userProfile.entity.User;
import co.istad.rentiq_api.features.userProfile.enums.AccountStatus;
import co.istad.rentiq_api.features.userProfile.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int BROADCAST_BATCH_SIZE = 250;
    private static final int KEYCLOAK_ROLE_PAGE_SIZE = 250;
    private static final ZoneOffset REPORTING_ZONE = ZoneOffset.UTC;

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final UserRepository userRepository;
    private final Keycloak keycloak;
    private final KeycloakAdminClientProps keycloakProps;

    @Override
    @Transactional
    public void notifyUser(
            String recipientUserId,
            NotificationType type,
            String title,
            String body,
            NotificationReferenceType referenceType,
            UUID referenceId
    ) {
        Notification notification = Notification.builder()
                .userId(recipientUserId)
                .notificationType(type)
                .title(title)
                .body(body)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();
        notificationRepository.save(NotificationPersistenceMapper.prepareForPersistence(notification));
    }

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
        List<String> recipientIds = request.audienceType() == BroadcastAudienceType.SINGLE_USER
                ? List.of(requireEligibleSingleRecipient(request.userId()).getId())
                : resolveAllEligibleRecipientIds();

        int notificationCount = 0;
        for (int start = 0; start < recipientIds.size(); start += BROADCAST_BATCH_SIZE) {
            int end = Math.min(start + BROADCAST_BATCH_SIZE, recipientIds.size());
            List<Notification> batch = recipientIds.subList(start, end).stream()
                    .map(userId -> NotificationPersistenceMapper.prepareForPersistence(
                            notificationMapper.toEntity(request, userId)))
                    .toList();
            notificationCount += notificationRepository.saveAll(batch).size();
        }

        return new BroadcastNotificationResponse(
                recipientIds.size(),
                notificationCount
        );
    }

    private User requireEligibleSingleRecipient(String userId) {
        String normalizedUserId = userId.trim();
        User user = userRepository.findById(normalizedUserId)
                .orElseThrow(() -> new NotFoundException("User", normalizedUserId));
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new InvalidStateException(
                    "User", user.getAccountStatus(), "Broadcast recipient must be active");
        }

        try {
            var userResource = keycloak.realm(keycloakProps.getTargetRealm()).users().get(normalizedUserId);
            UserRepresentation identity = userResource.toRepresentation();
            Set<String> roles = userResource.roles().realmLevel().listAll().stream()
                    .map(role -> role.getName())
                    .collect(java.util.stream.Collectors.toSet());
            boolean eligibleRole = roles.contains(RoleEnum.USER.name()) || roles.contains(RoleEnum.VENDOR.name());
            if (!Boolean.TRUE.equals(identity.isEnabled()) || !eligibleRole || roles.contains(RoleEnum.ADMIN.name())) {
                throw new InvalidStateException(
                        "User", user.getAccountStatus(), "Broadcast recipient is not an eligible USER or VENDOR account");
            }
            return user;
        } catch (InvalidStateException exception) {
            throw exception;
        } catch (jakarta.ws.rs.NotFoundException exception) {
            throw new NotFoundException("Identity user", normalizedUserId);
        } catch (RuntimeException exception) {
            throw new KeycloakOperationException("Failed to validate broadcast recipient " + normalizedUserId, exception);
        }
    }

    private List<String> resolveAllEligibleRecipientIds() {
        Set<String> eligibleRoleIds = new LinkedHashSet<>();
        eligibleRoleIds.addAll(loadEnabledRoleMemberIds(RoleEnum.USER));
        eligibleRoleIds.addAll(loadEnabledRoleMemberIds(RoleEnum.VENDOR));
        eligibleRoleIds.removeAll(loadRoleMemberIds(RoleEnum.ADMIN));

        List<String> orderedIds = eligibleRoleIds.stream().sorted().toList();
        List<String> eligibleLocalIds = new ArrayList<>();
        for (int start = 0; start < orderedIds.size(); start += BROADCAST_BATCH_SIZE) {
            int end = Math.min(start + BROADCAST_BATCH_SIZE, orderedIds.size());
            userRepository.findAllByIdInAndAccountStatus(
                            orderedIds.subList(start, end), AccountStatus.ACTIVE)
                    .stream()
                    .map(User::getId)
                    .sorted()
                    .forEach(eligibleLocalIds::add);
        }
        return List.copyOf(eligibleLocalIds);
    }

    private Set<String> loadEnabledRoleMemberIds(RoleEnum role) {
        return loadRoleMembers(role).stream()
                .filter(user -> Boolean.TRUE.equals(user.isEnabled()))
                .map(UserRepresentation::getId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> loadRoleMemberIds(RoleEnum role) {
        return loadRoleMembers(role).stream()
                .map(UserRepresentation::getId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private List<UserRepresentation> loadRoleMembers(RoleEnum role) {
        try {
            List<UserRepresentation> members = new ArrayList<>();
            for (int first = 0; ; first += KEYCLOAK_ROLE_PAGE_SIZE) {
                List<UserRepresentation> page = keycloak.realm(keycloakProps.getTargetRealm())
                        .roles().get(role.name()).getUserMembers(first, KEYCLOAK_ROLE_PAGE_SIZE);
                members.addAll(page);
                if (page.size() < KEYCLOAK_ROLE_PAGE_SIZE) {
                    return members;
                }
            }
        } catch (RuntimeException exception) {
            throw new KeycloakOperationException("Failed to resolve " + role + " broadcast audience", exception);
        }
    }

    @Override
    public Page<AdminNotificationResponse> adminListNotifications(
            String userId,
            NotificationType type,
            Boolean read,
            NotificationReferenceType referenceType,
            UUID referenceId,
            LocalDate createdFrom,
            LocalDate createdTo,
            Pageable pageable
    ) {
        if (createdFrom != null && createdTo != null && createdTo.isBefore(createdFrom)) {
            throw new InvalidOperationException("createdFrom date must not be after createdTo date");
        }

        OffsetDateTime fromInclusive = createdFrom == null
                ? null : createdFrom.atStartOfDay(REPORTING_ZONE).toOffsetDateTime();
        OffsetDateTime toExclusive = createdTo == null
                ? null : createdTo.plusDays(1).atStartOfDay(REPORTING_ZONE).toOffsetDateTime();

        Pageable boundedPageable = pageable.getPageSize() > MAX_PAGE_SIZE
                ? PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort())
                : pageable;

        return notificationRepository
                .findAll(
                        NotificationSpecification.adminFilter(
                                userId, type, read, referenceType, referenceId, fromInclusive, toExclusive),
                        boundedPageable)
                .map(notificationMapper::toAdminResponse);
    }

    @Override
    public AdminNotificationResponse adminGetNotification(UUID id) {
        return notificationRepository.findById(id)
                .map(notificationMapper::toAdminResponse)
                .orElseThrow(() -> new NotFoundException("Notification", id));
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
