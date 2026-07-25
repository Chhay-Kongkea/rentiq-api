package co.istad.rentiq_api.features.booking.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ItemAvailabilityRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    public boolean hasOverlap(UUID itemId, LocalDate start, LocalDate end) {
        Number count = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM item_availability " +
                                "WHERE item_id = :itemId " +
                                "AND blocked_range && daterange(CAST(:start AS date), CAST(:end AS date), '[]')")
                .setParameter("itemId", itemId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult();
        return count.longValue() > 0;
    }

    @Transactional
    public void block(UUID itemId, LocalDate start, LocalDate end, String reason, UUID bookingId) {
        entityManager.createNativeQuery(
                        "INSERT INTO item_availability (id, item_id, blocked_range, reason, booking_id) " +
                                "VALUES (gen_random_uuid(), :itemId, daterange(CAST(:start AS date), CAST(:end AS date), '[]'), :reason, :bookingId)")
                .setParameter("itemId", itemId)
                .setParameter("start", start)
                .setParameter("end", end)
                .setParameter("reason", reason)
                .setParameter("bookingId", bookingId)
                .executeUpdate();
    }

    @Transactional
    public void releaseByBookingId(UUID bookingId) {
        entityManager.createNativeQuery("DELETE FROM item_availability WHERE booking_id = :bookingId")
                .setParameter("bookingId", bookingId)
                .executeUpdate();
    }
}