package co.istad.rentiq_api.features.bookings.repository;

import co.istad.rentiq_api.features.bookings.entity.Booking;
import co.istad.rentiq_api.features.bookings.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<Booking> findByOwnerIdOrderByCreatedAtDesc(String ownerId);

    List<Booking> findByItem_IdAndStatusInOrderByRentalStartAsc(UUID itemId, List<BookingStatus> statuses);

    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

    @Query("""
            select count(b) > 0 from Booking b
            where b.item.id = :itemId
              and b.status in :activeStatuses
              and b.rentalStart <= :end
              and b.rentalEnd >= :start
            """)
    boolean existsOverlappingBooking(@Param("itemId") UUID itemId,
                                      @Param("start") LocalDate start,
                                      @Param("end") LocalDate end,
                                      @Param("activeStatuses") List<BookingStatus> activeStatuses);

    @Query("""
            select b from Booking b
            where b.ownerId = :ownerId
              and (:from is null or b.rentalEnd >= :from)
              and (:to is null or b.rentalStart <= :to)
            order by b.rentalStart asc
            """)
    List<Booking> findScheduleByOwnerId(@Param("ownerId") String ownerId,
                                         @Param("from") LocalDate from,
                                         @Param("to") LocalDate to);
}
