package co.istad.rentiq_api.features.auth.controller;

import co.istad.rentiq_api.features.auth.dto.request.*;
import co.istad.rentiq_api.features.auth.dto.response.MessageResponse;
import co.istad.rentiq_api.features.auth.dto.response.RegisterResponse;
import co.istad.rentiq_api.features.auth.dto.response.TokenResponse;
import co.istad.rentiq_api.features.auth.service.AuthService;
import co.istad.rentiq_api.security.AuthUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return authService.register(request);
    }


    @GetMapping("/login")
    public void login(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/keycloak");
    }
    @PostMapping("user/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
    /*
     * Called by the frontend after OAuth login succeeds.
     */
    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(
            OAuth2AuthenticationToken authentication
    ) {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("userId", authentication.getPrincipal().getAttribute("sub"));

        result.put("username", authentication.getPrincipal().getAttribute("preferred_username"));

        result.put("email", authentication.getPrincipal().getAttribute("email"));

        result.put("firstName", authentication.getPrincipal().getAttribute("given_name"));

        result.put("lastName", authentication.getPrincipal().getAttribute("family_name"));

        return result;
    }

    @PostMapping("/refresh-token")
    public TokenResponse refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return authService.refreshToken(request);
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        authService.forgotPassword(request);

        return MessageResponse.builder()
                .message(
                        "If an account with that email exists, " +
                                "a reset link has been sent"
                )
                .build();
    }

    @GetMapping("/verify-email")
    public MessageResponse verifyEmailStatus(
            @RequestParam String email
    ) {
        boolean verified = authService.isEmailVerified(email);

        return MessageResponse.builder()
                .message(
                        verified
                                ? "Email is verified"
                                : "Email is not verified"
                )
                .build();
    }

    @PostMapping("/resend-verification-email")
    public MessageResponse resendVerificationEmail(
            @Valid @RequestBody ResendVerificationRequest request
    ) {
        authService.resendVerificationEmail(request);

        return MessageResponse.builder()
                .message(
                        "If the account exists and is unverified, " +
                                "a verification email has been sent"
                )
                .build();
    }

    @PostMapping("/change-password")
    public MessageResponse changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        String userId = AuthUtils.extractUserId();

        authService.changePassword(userId, request);

        return MessageResponse.builder()
                .message("Password changed successfully")
                .build();
    }
}