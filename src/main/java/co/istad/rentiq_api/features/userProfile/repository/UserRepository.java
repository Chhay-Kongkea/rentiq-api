package co.istad.rentiq_api.features.userProfile.repository;


import co.istad.rentiq_api.features.userProfile.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
}