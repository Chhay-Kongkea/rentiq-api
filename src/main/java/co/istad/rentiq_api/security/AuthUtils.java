package co.istad.rentiq_api.security;




import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

public final class AuthUtils {

    private AuthUtils() {}

    public static String extractUserId() {
        Authentication auth = getAuth();

        if (auth instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have been forbidden");
        }

        JwtAuthenticationToken jwtAuthenticationToken = (JwtAuthenticationToken) auth;

        System.out.println(jwtAuthenticationToken);

        return jwtAuthenticationToken.getToken().getSubject();
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
        Authentication auth = getAuth();
        if (auth instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have been forbidden");
        }
        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) auth;
        Object value = jwtAuth.getToken().getClaims().get(claimName);
        return value != null ? value.toString() : null;
    }

}