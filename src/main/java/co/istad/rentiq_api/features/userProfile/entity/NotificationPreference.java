package co.istad.rentiq_api.features.userProfile.entity;



import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @Column(name = "user_id", length = 255)
    private String userId;

    @Column(name = "booking_notifications")
    @Builder.Default
    private boolean bookingNotifications = true;

    @Column(name = "payment_notifications")
    @Builder.Default
    private boolean paymentNotifications = true;

    @Column(name = "marketing_notifications")
    @Builder.Default
    private boolean marketingNotifications = true;

    @Column(name = "email_notifications")
    @Builder.Default
    private boolean emailNotifications = true;

    @Column(name = "push_notifications")
    @Builder.Default
    private boolean pushNotifications = true;
}