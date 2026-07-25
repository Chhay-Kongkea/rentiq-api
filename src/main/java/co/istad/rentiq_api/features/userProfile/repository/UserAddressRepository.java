package co.istad.rentiq_api.features.userProfile.repository;


import co.istad.rentiq_api.features.userProfile.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAddressRepository extends JpaRepository<UserAddress, UUID> {

    List<UserAddress> findAllByUserIdOrderByIsDefaultDescCreatedAtDesc(String userId);

    Optional<UserAddress> findByIdAndUserId(UUID id, String userId);

    Optional<UserAddress> findByUserIdAndIsDefaultTrue(String userId);

    long countByUserId(String userId);
}