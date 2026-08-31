package co.istad.rentiq_api.security;




import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

public final class AuthUtils {

    private AuthUtils() {}

    public static String extractUserId() {
        return requireJwt().getToken().getSubject();
    }

    public static String extractJwt() {
        if (getAuth().getPrincipal() != null)
            return ((Jwt) getAuth().getPrincipal()).getTokenValue();
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You have been unauthorized");
    }

    public static Authentication getAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }


    public static String extractUsername() {
        return claim("preferred_username");
    }

    public static String extractEmail() {
        return claim("email");
    }

    public static String extractFirstName() {
        return claim("given_name");
    }

    public static String extractLastName() {
        return claim("family_name");
    }

    private static String claim(String claimName) {
        Object value = requireJwt().getToken().getClaims().get(claimName);
        return value != null ? value.toString() : null;
    }

    /**
     * Every helper in this class expects a bearer-JWT-authenticated principal — this REST API
     * is bearer-JWT only; the separate browser OAuth2 login flow exists solely to bootstrap the
     * Keycloak handshake and never reaches these business-identity helpers. Explicitly rejects
     * anonymous and any other non-JWT Authentication (instead of an unchecked cast that would
     * surface as an unhandled ClassCastException/500 for an unexpected principal type).
     */
    private static JwtAuthenticationToken requireJwt() {
        Authentication auth = getAuth();

        if (!(auth instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have been forbidden");
        }

        return jwtAuthenticationToken;
    }

}
