package co.istad.rentiq_api.features.bookings.repository;

import co.istad.rentiq_api.features.bookings.entity.BookingStatusHistory;
import co.istad.rentiq_api.features.bookings.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BookingStatusHistoryRepository extends JpaRepository<BookingStatusHistory, UUID> {

    List<BookingStatusHistory> findByBookingIdOrderByCreatedAtAsc(UUID bookingId);

    long countByBooking_OwnerIdAndOldStatusAndNewStatus(String ownerId, BookingStatus oldStatus, BookingStatus newStatus);

    long countByBooking_OwnerIdAndNewStatus(String ownerId, BookingStatus newStatus);


    @Query(value = """
            select percentile_cont(0.5) within group (
                order by extract(epoch from (first_action.first_changed_at - b.created_at))
            )
            from bookings b
            join lateral (
                select min(h.created_at) as first_changed_at
                from booking_status_history h
                where h.booking_id = b.id
                  and h.changed_by = b.owner_id
            ) first_action on first_action.first_changed_at is not null
            where b.owner_id = :ownerId
            """, nativeQuery = true)
    Double findMedianResponseSeconds(@Param("ownerId") String ownerId);

}
