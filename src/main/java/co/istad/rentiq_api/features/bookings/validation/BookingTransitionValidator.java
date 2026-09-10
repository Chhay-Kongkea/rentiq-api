package co.istad.rentiq_api.features.bookings.validation;

import co.istad.rentiq_api.features.bookings.enums.BookingStatus;
import co.istad.rentiq_api.features.bookings.exception.InvalidBookingOperationException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Backend audit P0-4 — the single canonical booking state machine. Both the normal
 * customer/vendor status-update path and the Admin status-update path go through this same
 * topology check: Admin gets elevated AUTHORIZATION (it doesn't have to be the specific
 * customer/owner on the booking to trigger an already-legal edge) but never a wider set of
 * reachable states. This class knows nothing about roles — "who" may trigger an already-legal
 * edge is decided by the caller (see {@code BookingServiceImpl}).
 *
 * There is currently no documented Admin override that widens this topology (e.g. force-jumping
 * a terminal booking back to an active state). If one is ever needed, it must be modeled as its
 * own explicit, separately-authorized operation — never as a bypass of this validator.
 */
@Component
public class BookingTransitionValidator {

    private static final Map<BookingStatus, Set<BookingStatus>> ALLOWED_TRANSITIONS = buildTransitions();

    // APPROVED -> RENTED exists in the lifecycle, but is only ever reached by scanning the
    // pickup QR code (proof of physical handoff) — never through the generic status-update
    // endpoint, for a normal caller or for Admin.
    private static final Set<BookingStatus> QR_ONLY_TARGETS = EnumSet.of(BookingStatus.RENTED);

    private static Map<BookingStatus, Set<BookingStatus>> buildTransitions() {
        Map<BookingStatus, Set<BookingStatus>> transitions = new EnumMap<>(BookingStatus.class);

        transitions.put(BookingStatus.PENDING,
                EnumSet.of(BookingStatus.APPROVED, BookingStatus.REJECTED, BookingStatus.CANCELLED));
        transitions.put(BookingStatus.APPROVED,
                EnumSet.of(BookingStatus.CANCELLED, BookingStatus.RENTED));
        transitions.put(BookingStatus.RENTED,
                EnumSet.of(BookingStatus.COMPLETED));

        // Terminal states — no outgoing transitions at all (requirement #6: completed/
        // cancelled/rejected/expired must never transition back, admin included).
        transitions.put(BookingStatus.COMPLETED, EnumSet.noneOf(BookingStatus.class));
        transitions.put(BookingStatus.REJECTED, EnumSet.noneOf(BookingStatus.class));
        transitions.put(BookingStatus.CANCELLED, EnumSet.noneOf(BookingStatus.class));
        transitions.put(BookingStatus.EXPIRED, EnumSet.noneOf(BookingStatus.class));

        return Map.copyOf(transitions);
    }

    public boolean isTopologicallyReachable(BookingStatus current, BookingStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(target);
    }

    /**
     * Validates {@code target} is reachable from {@code current} through the generic
     * status-update endpoint. Throws {@link InvalidBookingOperationException} (mapped to
     * 409 Conflict) for any edge outside the base state machine — including a same-status
     * no-op, any edge out of a terminal state, and the QR-only APPROVED to RENTED edge.
     */
    public void assertReachableViaStatusEndpoint(BookingStatus current, BookingStatus target) {
        if (!isTopologicallyReachable(current, target)) {
            throw new InvalidBookingOperationException(
                    "Cannot transition booking from " + current + " to " + target);
        }

        if (QR_ONLY_TARGETS.contains(target)) {
            throw new InvalidBookingOperationException(
                    "Use the pickup QR code scan to mark a booking as picked up");
        }
    }
}
