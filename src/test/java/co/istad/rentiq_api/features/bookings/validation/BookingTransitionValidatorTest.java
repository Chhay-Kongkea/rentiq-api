package co.istad.rentiq_api.features.bookings.validation;

import co.istad.rentiq_api.features.bookings.enums.BookingStatus;
import co.istad.rentiq_api.features.bookings.exception.InvalidBookingOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Backend audit P0-4 — this is the single canonical booking state machine; every valid and
 * representative invalid transition is exercised here directly, independent of who the caller
 * is (role gating is a separate concern layered on top in BookingServiceImpl).
 */
class BookingTransitionValidatorTest {

    private final BookingTransitionValidator validator = new BookingTransitionValidator();

    @ParameterizedTest(name = "{0} -> {1} is valid")
    @CsvSource({
            "PENDING, APPROVED",
            "PENDING, REJECTED",
            "PENDING, CANCELLED",
            "APPROVED, CANCELLED",
            "RENTED, COMPLETED",
    })
    void validTransitions_areAccepted(BookingStatus current, BookingStatus target) {
        assertThatCode(() -> validator.assertReachableViaStatusEndpoint(current, target))
                .doesNotThrowAnyException();
    }

    // ---------------------------------------------------------------
    // The exact illegal jumps named in the P0-4 audit findings — must never become possible
    // simply because the caller is Admin. BookingServiceImpl routes admin through this same
    // validator, so these prove the fix at the state-machine level.
    // ---------------------------------------------------------------

    @Test
    void pendingToCompleted_isRejected() {
        assertThatThrownBy(() -> validator.assertReachableViaStatusEndpoint(BookingStatus.PENDING, BookingStatus.COMPLETED))
                .isInstanceOf(InvalidBookingOperationException.class);
    }

    @Test
    void rejectedToApproved_isRejected() {
        // "REJECTED -> ACTIVE" in the audit's phrasing; REJECTED is terminal, so REJECTED can
        // never move to any active status (APPROVED/RENTED) again.
        assertThatThrownBy(() -> validator.assertReachableViaStatusEndpoint(BookingStatus.REJECTED, BookingStatus.APPROVED))
                .isInstanceOf(InvalidBookingOperationException.class);
    }

    @Test
    void completedToPending_isRejected() {
        assertThatThrownBy(() -> validator.assertReachableViaStatusEndpoint(BookingStatus.COMPLETED, BookingStatus.PENDING))
                .isInstanceOf(InvalidBookingOperationException.class);
    }

    @Test
    void cancelledToRented_isRejected() {
        assertThatThrownBy(() -> validator.assertReachableViaStatusEndpoint(BookingStatus.CANCELLED, BookingStatus.RENTED))
                .isInstanceOf(InvalidBookingOperationException.class);
    }

    // ---------------------------------------------------------------
    // APPROVED -> RENTED is a real edge in the lifecycle but only reachable via QR scan, never
    // through the generic status endpoint — for a normal caller or Admin.
    // ---------------------------------------------------------------

    @Test
    void approvedToRented_isRejectedViaTheGenericStatusEndpoint() {
        assertThatThrownBy(() -> validator.assertReachableViaStatusEndpoint(BookingStatus.APPROVED, BookingStatus.RENTED))
                .isInstanceOf(InvalidBookingOperationException.class)
                .hasMessageContaining("QR code");
    }

    @Test
    void isTopologicallyReachable_stillReportsApprovedToRentedAsALifecycleEdge() {
        // The edge genuinely exists (it's how QR-scan pickup is modeled) — it's the *generic
        // endpoint* that blocks it, not the base topology.
        org.assertj.core.api.Assertions.assertThat(
                        validator.isTopologicallyReachable(BookingStatus.APPROVED, BookingStatus.RENTED))
                .isTrue();
    }

    // ---------------------------------------------------------------
    // Terminal states never transition back out, to anything — requirement #6.
    // ---------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = {"COMPLETED", "REJECTED", "CANCELLED", "EXPIRED"})
    void terminalStates_haveNoOutgoingTransitions(BookingStatus terminal) {
        for (BookingStatus target : BookingStatus.values()) {
            assertThatThrownBy(() -> validator.assertReachableViaStatusEndpoint(terminal, target))
                    .as("%s -> %s must be rejected", terminal, target)
                    .isInstanceOf(InvalidBookingOperationException.class);
        }
    }

    @ParameterizedTest
    @EnumSource(BookingStatus.class)
    void sameStatusNoOp_isRejected(BookingStatus status) {
        assertThatThrownBy(() -> validator.assertReachableViaStatusEndpoint(status, status))
                .isInstanceOf(InvalidBookingOperationException.class);
    }
}
