package co.istad.rentiq_api.features.booking.repository;

import co.istad.rentiq_api.features.booking.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Page<Booking> findByCustomerId(String customerId, Pageable pageable);

    Page<Booking> findByOwnerId(String ownerId, Pageable pageable);

    boolean existsByBookingRef(String bookingRef);

    @Query("""
        SELECT COUNT(b) > 0
        FROM Booking b
        WHERE b.itemId = :itemId
        AND b.status NOT IN ('CANCELLED', 'REJECTED')
        AND b.rentalStart <= :rentalEnd
        AND b.rentalEnd >= :rentalStart
    """)
    boolean existsOverlappingBooking(
            UUID itemId,
            LocalDate rentalStart,
            LocalDate rentalEnd
    );

    @Query("SELECT b FROM Booking b WHERE b.ownerId = :ownerId " +
            "AND b.rentalEnd >= :from AND b.rentalStart <= :to " +
            "AND b.status IN :statuses ORDER BY b.rentalStart")
    List<Booking> findScheduleForOwner(@Param("ownerId") String ownerId,
                                        @Param("from") LocalDate from,
                                        @Param("to") LocalDate to,
                                        @Param("statuses") List<String> statuses);
}