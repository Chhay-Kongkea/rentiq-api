//package co.istad.rentiq_api.features.auth.controller;
//
//
//import co.istad.rentiq_api.features.auth.service.AuthService;
//import co.istad.rentiq_api.features.auth.dto.request.RegisterRequest;
//import co.istad.rentiq_api.features.auth.dto.response.RegisterResponse;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/v1/auth")
//@RequiredArgsConstructor
//@Slf4j
//public class AuthController {
//
//    private final AuthService authService;
//
//    @ResponseStatus(HttpStatus.CREATED)
//    @PostMapping("/register")
//    public RegisterResponse register(@Valid @RequestBody RegisterRequest request){
//
//        try {
//            // Call external service
//            return authService.Register(request);
//        } catch (Exception e) {
//            log.error("Error while calling external service", e);
//            throw e;
//        }
//
//
//   }
//
//}


package co.istad.rentiq_api.features.auth.controller;

import co.istad.rentiq_api.features.auth.dto.request.*;
import co.istad.rentiq_api.features.auth.dto.response.MessageResponse;
import co.istad.rentiq_api.features.auth.dto.response.RegisterResponse;
import co.istad.rentiq_api.features.auth.dto.response.TokenResponse;
import co.istad.rentiq_api.features.auth.service.AuthService;
import co.istad.rentiq_api.security.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register")
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/logout")
    public void logout(@Valid @RequestBody LogoutRequest request) {
        AuthUtils.extractUserId(); // enforces caller is authenticated
        authService.logout(request);
    }

    @PostMapping("/refresh-token")
    public TokenResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refreshToken(request);
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return MessageResponse.builder()
                .message("If an account with that email exists, a reset link has been sent")
                .build();
    }

    @GetMapping("/verify-email")
    public MessageResponse verifyEmailStatus(@RequestParam String email) {
        boolean verified = authService.isEmailVerified(email);
        return MessageResponse.builder()
                .message(verified ? "Email is verified" : "Email is not verified")
                .build();
    }

    @PostMapping("/resend-verification-email")
    public MessageResponse resendVerificationEmail(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerificationEmail(request);
        return MessageResponse.builder()
                .message("If the account exists and is unverified, a verification email has been sent")
                .build();
    }

    @PostMapping("/change-password")
    public MessageResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String userId = AuthUtils.extractUserId();
        authService.changePassword(userId, request);
        return MessageResponse.builder().message("Password changed successfully").build();
    }
}