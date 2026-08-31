package co.istad.rentiq_api.features.bookingDispute.repository;

import co.istad.rentiq_api.features.bookingDispute.entity.BookingDispute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BookingDisputeRepository extends JpaRepository<BookingDispute, UUID> {
    List<BookingDispute> findByBookingIdOrderByCreatedAtDesc(UUID bookingId);
    Page<BookingDispute> findByStatus(String status, Pageable pageable);
    long countByStatus(String status);

    @Query("select count(distinct d.bookingId) from BookingDispute d where d.status = :status")
    long countDistinctBookingIdByStatus(@Param("status") String status);

    Page<BookingDispute> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
