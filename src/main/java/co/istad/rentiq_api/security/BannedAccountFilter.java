package co.istad.rentiq_api.security;

import co.istad.rentiq_api.common.dto.ApiErrorResponse;
import co.istad.rentiq_api.features.userProfile.enums.AccountStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;


@Component
@RequiredArgsConstructor
public class BannedAccountFilter extends OncePerRequestFilter {

    private final AccountStatusGuard accountStatusGuard;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String userId = jwtAuth.getToken().getSubject();

            // Single DB read; database is the source of truth for account status,
            // never the JWT claims. Covers USER, VENDOR and ADMIN principals alike.
            AccountStatus status = accountStatusGuard.resolveStatus(userId);

            if (status == AccountStatus.BANNED) {
                writeStatusResponse(response, request, "ACCOUNT_BANNED", "Your account has been banned");
                return;
            }

            if (status == AccountStatus.SUSPENDED) {
                writeStatusResponse(response, request, "ACCOUNT_SUSPENDED", "Your account has been suspended");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeStatusResponse(
            HttpServletResponse response,
            HttpServletRequest request,
            String code,
            String message
    ) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                code,
                message,
                request.getRequestURI(),
                Map.of()
        );

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
