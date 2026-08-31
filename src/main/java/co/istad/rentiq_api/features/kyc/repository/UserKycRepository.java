package co.istad.rentiq_api.features.kyc.repository;


import co.istad.rentiq_api.features.kyc.entity.UserKyc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserKycRepository extends JpaRepository<UserKyc, UUID> {

    Optional<UserKyc> findByUserId(String userId);

    boolean existsByUserId(String userId);

    Page<UserKyc> findAllByVerificationStatus(String status, Pageable pageable);

    long countByVerificationStatus(String status);

    Page<UserKyc> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
