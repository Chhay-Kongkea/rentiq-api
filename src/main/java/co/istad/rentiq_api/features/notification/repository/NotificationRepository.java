package co.istad.rentiq_api.features.notification.repository;

import co.istad.rentiq_api.features.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository
        extends JpaRepository<Notification, UUID> {

    Page<Notification>
    findAllByUserIdOrderByCreatedAtDesc(
            String userId,
            Pageable pageable
    );

    Optional<Notification>
    findByIdAndUserId(
            UUID id,
            String userId
    );

    long countByUserIdAndReadFalse(
            String userId
    );

    @Modifying
    @Query("""
            UPDATE Notification notification
            SET notification.read = true,
                notification.readAt = :readAt
            WHERE notification.userId = :userId
              AND notification.read = false
            """)
    int markAllAsRead(
            @Param("userId") String userId,
            @Param("readAt") OffsetDateTime readAt
    );

    long deleteByIdAndUserId(
            UUID id,
            String userId
    );
}