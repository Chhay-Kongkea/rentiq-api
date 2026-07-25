package co.istad.rentiq_api.features.booking.repository;

import co.istad.rentiq_api.features.booking.entity.BookingQrCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BookingQrCodeRepository extends JpaRepository<BookingQrCode, UUID> {
    Optional<BookingQrCode> findByBookingId(UUID bookingId);
    Optional<BookingQrCode> findByQrTokenAndIsValidTrue(String qrToken);
}