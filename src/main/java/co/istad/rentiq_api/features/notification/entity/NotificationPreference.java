package co.istad.rentiq_api.features.notification.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {

    @Id
    @Column(name = "user_id", length = 255)
    private String userId;

    @Column(name = "booking_notifications", nullable = false)
    @Builder.Default
    private boolean bookingNotifications = true;

    @Column(name = "payment_notifications", nullable = false)
    @Builder.Default
    private boolean paymentNotifications = true;

    @Column(name = "marketing_notifications", nullable = false)
    @Builder.Default
    private boolean marketingNotifications = true;

    @Column(name = "email_notifications", nullable = false)
    @Builder.Default
    private boolean emailNotifications = true;

    @Column(name = "push_notifications", nullable = false)
    @Builder.Default
    private boolean pushNotifications = true;
}