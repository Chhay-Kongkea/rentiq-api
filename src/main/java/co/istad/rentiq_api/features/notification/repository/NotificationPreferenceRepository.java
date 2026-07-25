package co.istad.rentiq_api.features.notification.repository;

import co.istad.rentiq_api.features.notification.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, String> {

}