package co.istad.rentiq_api.features.auth.service;


import co.istad.rentiq_api.features.auth.dto.request.*;
import co.istad.rentiq_api.features.auth.dto.response.RegisterResponse;
import co.istad.rentiq_api.features.auth.dto.response.TokenResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);
    TokenResponse login(LoginRequest request);

    void logout(LogoutRequest request);

    TokenResponse refreshToken(RefreshTokenRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void changePassword(String keycloakUserId, ChangePasswordRequest request);

    void resendVerificationEmail(ResendVerificationRequest request);

    boolean isEmailVerified(String email);
}
