package co.istad.rentiq_api.features.userProfile.repository;



import co.istad.rentiq_api.features.userProfile.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, String> {
}