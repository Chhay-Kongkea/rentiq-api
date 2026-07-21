package co.istad.rentiq_api.features.booking.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class BookingStatusTransitionValidator {

    public enum Actor { CUSTOMER, OWNER, ADMIN }

    private static final Map<Actor, Map<String, Set<String>>> TRANSITIONS = Map.of(
            Actor.CUSTOMER, Map.of(
                    "PENDING", Set.of("CANCELLED"),
                    "APPROVED", Set.of("CANCELLED")
            ),
            Actor.OWNER, Map.of(
                    "PENDING", Set.of("APPROVED", "REJECTED"),
                    "APPROVED", Set.of("RENTED"),
                    "RENTED", Set.of("COMPLETED")
            ),
            Actor.ADMIN, Map.of(
                    "PENDING", Set.of("APPROVED", "REJECTED", "CANCELLED", "EXPIRED"),
                    "APPROVED", Set.of("RENTED", "CANCELLED", "EXPIRED"),
                    "RENTED", Set.of("COMPLETED", "CANCELLED")
            )
    );

    public boolean isAllowed(Actor actor, String from, String to) {
        return TRANSITIONS.getOrDefault(actor, Map.of())
                .getOrDefault(from, Set.of())
                .contains(to);
    }
}