package co.istad.rentiq_api.features.bookingInspection.repository;


import co.istad.rentiq_api.features.bookingInspection.entity.BookingInspection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BookingInspectionRepository extends JpaRepository<BookingInspection, UUID> {
    Optional<BookingInspection> findByBookingId(UUID bookingId);
    boolean existsByBookingId(UUID bookingId);
}