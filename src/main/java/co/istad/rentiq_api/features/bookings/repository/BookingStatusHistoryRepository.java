package co.istad.rentiq_api.features.bookings.repository;

import co.istad.rentiq_api.features.bookings.entity.BookingStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingStatusHistoryRepository extends JpaRepository<BookingStatusHistory, UUID> {

    List<BookingStatusHistory> findByBookingIdOrderByCreatedAtAsc(UUID bookingId);

}
