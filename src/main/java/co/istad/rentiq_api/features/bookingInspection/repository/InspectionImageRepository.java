package co.istad.rentiq_api.features.bookingInspection.repository;


import co.istad.rentiq_api.features.bookingInspection.entity.InspectionImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InspectionImageRepository extends JpaRepository<InspectionImage, UUID> {
    List<InspectionImage> findByInspectionIdOrderByCreatedAtAsc(UUID inspectionId);
    Optional<InspectionImage> findByIdAndInspectionId(UUID id, UUID inspectionId);
}