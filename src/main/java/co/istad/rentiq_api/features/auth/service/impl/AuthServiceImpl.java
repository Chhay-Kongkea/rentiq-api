////package co.istad.rentiq_api.features.auth;
////
////import co.istad.rentiq_api.config.props.KeycloakAdminClientProps;
////import co.istad.rentiq_api.features.auth.service.AuthService;
////import co.istad.rentiq_api.features.auth.RoleEnum;
////import co.istad.rentiq_api.features.auth.dto.request.RegisterRequest;
////import co.istad.rentiq_api.features.auth.dto.response.RegisterResponse;
////import jakarta.ws.rs.core.Response;
////import lombok.RequiredArgsConstructor;
////import lombok.extern.slf4j.Slf4j;
////
////import org.keycloak.admin.client.Keycloak;
////import org.keycloak.admin.client.resource.RolesResource;
////import org.keycloak.admin.client.resource.UserResource;
////import org.keycloak.admin.client.resource.UsersResource;
////import org.keycloak.representations.idm.CredentialRepresentation;
////import org.keycloak.representations.idm.RoleRepresentation;
////import org.keycloak.representations.idm.UserRepresentation;
////import org.springframework.http.HttpStatus;
////import org.springframework.stereotype.Service;
////import org.springframework.web.server.ResponseStatusException;
////
////import java.util.HashMap;
////import java.util.List;
////import java.util.Map;
////
////@Service
////@RequiredArgsConstructor
////@Slf4j
////public class AuthServiceImpl implements AuthService {
////
////    private final Keycloak keycloak;
////    private final KeycloakAdminClientProps props;
////
////
////    @Override
////    public RegisterResponse Register(RegisterRequest registerRequest) {
////
////
////        //validate password
////
////        if(!registerRequest.password().equals(registerRequest.confirmPassword())){
////            throw new ResponseStatusException(
////                    HttpStatus.BAD_REQUEST, "Password is not match"
////            );
////        }
////
////
////
////        UserRepresentation user = new UserRepresentation();
////        user.setUsername(registerRequest.username());
////        user.setEmail(registerRequest.email());
////        user.setFirstName(registerRequest.firstName());
////        user.setLastName(registerRequest.lastName());
////
////
////
////
////        // set credential
////        CredentialRepresentation credential = new CredentialRepresentation();
////        credential.setType(CredentialRepresentation.PASSWORD);
////        credential.setValue(registerRequest.password());
////        user.setCredentials(List.of(credential));
////
////
////        user.setEnabled(true);
////        user.setEmailVerified(false);
////
////
////        // call keycloak api
////
////        UsersResource usersResource = keycloak.realm(props.getTargetRealm()).users();
////
////
////
////
////
//////        try(Response response = usersResource.create(user)){
////
////        try {
////            Response response = usersResource.create(user);
////            log.info("Keycloak response status: {}", response.getStatus());
////            if(response.getStatus() == HttpStatus.CREATED.value()){
////                UserRepresentation createdUser = usersResource
////                        .search(user.getUsername())
////                        .getFirst();
////
////                log.info("Created User {}", createdUser.getId());
////
////
////
////
////
////
//////                studentProfileRepository.save(studentProfile);
////
////                // save user to database
////
////
////                UserResource userResource = keycloak.realm(props.getTargetRealm()).users().get(createdUser.getId());
////                userResource.sendVerifyEmail();
////
////                // start assign role
////
////                RolesResource rolesResource = keycloak.realm(props.getTargetRealm())
////                        .roles();
////
////                RoleRepresentation roleUser = rolesResource.get(RoleEnum.USER.name()).toRepresentation();
////
////                RoleRepresentation roleAdmin = rolesResource.get(RoleEnum.ADMIN.name()).toRepresentation();
////
////                log.info("Check role user from keycloak: {}",roleUser);
////                log.info("Check role admin from keycloak: {}",roleAdmin);
////
////
////                userResource.roles().realmLevel().add(List.of(roleUser, roleAdmin));
////
////                return RegisterResponse.builder()
////                        .id(createdUser.getId())
////                        .username(createdUser.getUsername())
////                        .email(createdUser.getEmail())
////                        .firstName(createdUser.getFirstName())
////                        .lastName(createdUser.getLastName())
////                        .build();
////            }
////
////
////        } catch (Exception e) {
////            log.error("Failed to create user in Keycloak", e);
////            throw e;
////        }
////
////
////        return null;
////    }
////}
//
//
//
//package co.istad.rentiq_api.features.auth.service.impl;
//
//import co.istad.rentiq_api.config.props.KeycloakAdminClientProps;
//import co.istad.rentiq_api.features.auth.RoleEnum;
//import co.istad.rentiq_api.features.auth.dto.request.RegisterRequest;
//import co.istad.rentiq_api.features.auth.dto.response.RegisterResponse;
//import co.istad.rentiq_api.features.auth.service.AuthService;
//import jakarta.ws.rs.core.Response;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.keycloak.admin.client.Keycloak;
//import org.keycloak.admin.client.resource.RolesResource;
//import org.keycloak.admin.client.resource.UserResource;
//import org.keycloak.admin.client.resource.UsersResource;
//import org.keycloak.representations.idm.CredentialRepresentation;
//import org.keycloak.representations.idm.RoleRepresentation;
//import org.keycloak.representations.idm.UserRepresentation;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import org.springframework.web.server.ResponseStatusException;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class AuthServiceImpl implements AuthService {
//
//    private final Keycloak keycloak;
//    private final KeycloakAdminClientProps props;
//
//
//    @Override
//
//    public RegisterResponse Register(RegisterRequest registerRequest) {
//
//        log.info("===== START REGISTER USER =====");
//        log.info("Register username: {}", registerRequest.username());
//        log.info("Register email: {}", registerRequest.email());
//
//
//        // Validate password
//        if (!registerRequest.password()
//                .equals(registerRequest.confirmPassword())) {
//
//            log.warn("Password mismatch for user {}", registerRequest.username());
//
//            throw new ResponseStatusException(
//                    HttpStatus.BAD_REQUEST,
//                    "Password does not match"
//            );
//        }
//
//
//        UserRepresentation user = new UserRepresentation();
//
//        user.setUsername(registerRequest.username());
//        user.setEmail(registerRequest.email());
//        user.setFirstName(registerRequest.firstName());
//        user.setLastName(registerRequest.lastName());
//        user.setEnabled(true);
//        user.setEmailVerified(false);
//
//
//        CredentialRepresentation credential =
//                new CredentialRepresentation();
//
//        credential.setType(CredentialRepresentation.PASSWORD);
//        credential.setValue(registerRequest.password());
//        credential.setTemporary(false);
//
//        user.setCredentials(List.of(credential));
//
//
//        log.info("Creating Keycloak user...");
//        log.info("Target realm: {}", props.getTargetRealm());
//
//
//        try {
//
//            UsersResource usersResource =
//                    keycloak
//                            .realm(props.getTargetRealm())
//                            .users();
//
//
//            log.info("Calling Keycloak create user API");
//
//
//            Response response = usersResource.create(user);
//
//
//            log.info(
//                    "Keycloak create user response status: {}",
//                    response.getStatus()
//            );
//
//
//            if (response.getStatus() != HttpStatus.CREATED.value()) {
//
//                String body = response.readEntity(String.class);
//
//                log.error(
//                        "Keycloak create user failed. Status: {}, Body: {}",
//                        response.getStatus(),
//                        body
//                );
//
//                throw new ResponseStatusException(
//                        HttpStatus.BAD_REQUEST,
//                        "Cannot create user"
//                );
//            }
//
//
//            log.info("User created successfully");
//
//
//            log.info("Searching created user...");
//
//
//            UserRepresentation createdUser =
//                    usersResource
//                            .search(
//                                    registerRequest.username(),
//                                    true
//                            )
//                            .stream()
//                            .findFirst()
//                            .orElseThrow(
//                                    () -> new RuntimeException(
//                                            "Created user not found"
//                                    )
//                            );
//
//
//            log.info(
//                    "Created user ID: {}",
//                    createdUser.getId()
//            );
//
//
//            UserResource userResource =
//                    usersResource.get(createdUser.getId());
//
//
//
//            log.info("Sending verify email...");
//
//            userResource.sendVerifyEmail();
//
//            log.info("Verify email sent");
//
//
//            log.info("Getting USER role from Keycloak...");
//
//
//            RolesResource rolesResource =
//                    keycloak
//                            .realm(props.getTargetRealm())
//                            .roles();
//
//
//            RoleRepresentation roleUser =
//                    rolesResourcecategories
//                            .get(RoleEnum.USER.name())
//                            .toRepresentation();
//
//
//            log.info(
//                    "Found role: {}",
//                    roleUser.getName()
//            );
//
//
//            log.info("Assigning USER role...");
//
//
//            userResource
//                    .roles()
//                    .realmLevel()
//                    .add(List.of(roleUser));
//
//
//            log.info(
//                    "USER role assigned successfully to {}",
//                    createdUser.getUsername()
//            );
//
//
//            log.info("===== REGISTER SUCCESS =====");
//
//
//            return RegisterResponse.builder()
//                    .id(createdUser.getId())
//                    .username(createdUser.getUsername())
//                    .email(createdUser.getEmail())
//                    .firstName(createdUser.getFirstName())
//                    .lastName(createdUser.getLastName())
//                    .build();
//
//
//        } catch (Exception e) {
//
//
//            log.error(
//                    "===== REGISTER FAILED ====="
//            );
//
//            log.error(
//                    "Exception type: {}",
//                    e.getClass().getName()
//            );
//
//            log.error(
//                    "Exception message: {}",
//                    e.getMessage()
//            );
//
//
//            throw new ResponseStatusException(
//                    HttpStatus.INTERNAL_SERVER_ERROR,
//                    "Registration failed",
//                    e
//
//        }
//    }
//}


package co.istad.rentiq_api.features.auth.service.impl;

import co.istad.rentiq_api.config.props.KeycloakAdminClientProps;
import co.istad.rentiq_api.features.auth.RoleEnum;
import co.istad.rentiq_api.features.auth.dto.request.*;
import co.istad.rentiq_api.features.auth.dto.response.KeycloakTokenResponse;
import co.istad.rentiq_api.features.auth.dto.response.RegisterResponse;
import co.istad.rentiq_api.features.auth.dto.response.TokenResponse;
import co.istad.rentiq_api.features.auth.exception.*;
import co.istad.rentiq_api.features.auth.service.AuthService;
import co.istad.rentiq_api.features.userProfile.service.UserProfileService;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final Keycloak keycloak;
    private final KeycloakAdminClientProps props;
    private final RestTemplate restTemplate;
    private final UserProfileService userProfileService;

    private static final String TOKEN_ENDPOINT = "%s/realms/%s/protocol/openid-connect/token";
    private static final String LOGOUT_ENDPOINT = "%s/realms/%s/protocol/openid-connect/logout";

    // ---------------------------------------------------------------
    // REGISTER
    // ---------------------------------------------------------------

    @Override
    public RegisterResponse register(RegisterRequest request) {

        log.info("Registering user: {}", request.username());

        if (!request.password().equals(request.confirmPassword())) {
            throw new PasswordMismatchException();
        }

        UsersResource usersResource = usersResource();

        if (usernameExists(usersResource, request.username())) {
            throw new UserAlreadyExistsException(request.username());
        }

        UserRepresentation user = buildUserRepresentation(request);

        Response response;
        try {
            response = usersResource.create(user);
        } catch (RuntimeException e) {
            log.error("Keycloak create user call failed for {}", request.username(), e);
            throw new KeycloakOperationException("Failed to create user in identity provider", e);
        }

        try (response) {
            if (response.getStatus() != HttpStatus.CREATED.value()) {
                String body = safeReadBody(response);
                log.error("Create user failed. status={}, body={}", response.getStatus(), body);

                if (response.getStatus() == HttpStatus.CONFLICT.value()) {
                    throw new UserAlreadyExistsException(request.username());
                }
                throw new KeycloakOperationException("Failed to create user: " + body);
            }
        }

        UserRepresentation createdUser = findByUsername(usersResource, request.username())
                .orElseThrow(() -> new KeycloakOperationException("User created but could not be retrieved"));

        UserResource userResource = usersResource.get(createdUser.getId());

        sendVerifyEmailSafely(userResource);
        assignRole(userResource, RoleEnum.USER);

        userProfileService.createProfileIfAbsent(createdUser.getId());

        log.info("User registered successfully: {}", createdUser.getId());

        return RegisterResponse.builder()
                .id(createdUser.getId())
                .username(createdUser.getUsername())
                .email(createdUser.getEmail())
                .firstName(createdUser.getFirstName())
                .lastName(createdUser.getLastName())
                .build();
    }

    // ---------------------------------------------------------------
    // LOGIN / LOGOUT / REFRESH
    // ---------------------------------------------------------------

//    @Override
//    public RegisterResponse Register(RegisterRequest request) {
//        return null;
//    }

    @Override
    public TokenResponse login(LoginRequest request) {

        log.info("Login attempt for user: {}", request.username());

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        form.add("username", request.username());
        form.add("password", request.password());

        return requestToken(form);
    }

    @Override
    public void logout(LogoutRequest request) {

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        form.add("refresh_token", request.refreshToken());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String url = String.format(LOGOUT_ENDPOINT, props.getServerUrl(), props.getTargetRealm());

        try {
            restTemplate.postForEntity(url, new HttpEntity<>(form, headers), Void.class);
            log.info("Logout succeeded");
        } catch (HttpClientErrorException e) {
            log.warn("Logout failed with client error: {}", e.getResponseBodyAsString());
            throw new AuthException(HttpStatus.BAD_REQUEST, "Invalid or expired refresh token");
        } catch (RestClientException e) {
            log.error("Logout call to Keycloak failed", e);
            throw new KeycloakOperationException("Failed to reach identity provider for logout", e);
        }
    }

    @Override
    public TokenResponse refreshToken(RefreshTokenRequest request) {

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        form.add("refresh_token", request.refreshToken());

        return requestToken(form);
    }

    // ---------------------------------------------------------------
    // PASSWORD / EMAIL
    // ---------------------------------------------------------------

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {

        UsersResource usersResource = usersResource();

        Optional<UserRepresentation> userOpt = findByEmail(usersResource, request.email());

        if (userOpt.isEmpty()) {
            // Do not reveal whether the email exists — respond as if it succeeded.
            log.info("Forgot password requested for unknown email");
            return;
        }

        UserResource userResource = usersResource.get(userOpt.get().getId());

        try {
            userResource.executeActionsEmail(List.of("UPDATE_PASSWORD"));
            log.info("Password reset email sent to user {}", userOpt.get().getId());
        } catch (RuntimeException e) {
            log.error("Failed to send password reset email", e);
            throw new KeycloakOperationException("Failed to send password reset email", e);
        }
    }

    @Override
    public void changePassword(String keycloakUserId, ChangePasswordRequest request) {

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new PasswordMismatchException();
        }

        UsersResource usersResource = usersResource();
        UserResource userResource = usersResource.get(keycloakUserId);

        UserRepresentation user;
        try {
            user = userResource.toRepresentation();
        } catch (jakarta.ws.rs.NotFoundException e) {
            throw new UserNotFoundException();
        }

        // Verify current password via a password-grant request.
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        form.add("username", user.getUsername());
        form.add("password", request.currentPassword());

        try {
            requestToken(form);
        } catch (InvalidCredentialsException e) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.newPassword());
        credential.setTemporary(false);

        try {
            userResource.resetPassword(credential);
            log.info("Password changed for user {}", keycloakUserId);
        } catch (RuntimeException e) {
            log.error("Failed to reset password for user {}", keycloakUserId, e);
            throw new KeycloakOperationException("Failed to change password", e);
        }
    }

    @Override
    public void resendVerificationEmail(ResendVerificationRequest request) {

        UsersResource usersResource = usersResource();

        Optional<UserRepresentation> userOpt = findByEmail(usersResource, request.email());

        if (userOpt.isEmpty()) {
            log.info("Resend verification requested for unknown email");
            return;
        }

        if (Boolean.TRUE.equals(userOpt.get().isEmailVerified())) {
            log.info("Resend verification skipped — already verified: {}", userOpt.get().getId());
            return;
        }

        sendVerifyEmailSafely(usersResource.get(userOpt.get().getId()));
    }

    @Override
    public boolean isEmailVerified(String email) {

        UserRepresentation user = findByEmail(usersResource(), email)
                .orElseThrow(UserNotFoundException::new);

        return Boolean.TRUE.equals(user.isEmailVerified());
    }

    // ---------------------------------------------------------------
    // PRIVATE HELPERS
    // ---------------------------------------------------------------

    private UsersResource usersResource() {
        return keycloak.realm(props.getTargetRealm()).users();
    }

    private boolean usernameExists(UsersResource usersResource, String username) {
        return findByUsername(usersResource, username).isPresent();
    }

    private Optional<UserRepresentation> findByUsername(UsersResource usersResource, String username) {
        try {
            return usersResource.search(username, true).stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(username))
                    .findFirst();
        } catch (RuntimeException e) {
            log.error("Failed to search user by username: {}", username, e);
            throw new KeycloakOperationException("Failed to search for user", e);
        }
    }

    private Optional<UserRepresentation> findByEmail(UsersResource usersResource, String email) {
        try {
            return usersResource.searchByEmail(email, true).stream().findFirst();
        } catch (RuntimeException e) {
            log.error("Failed to search user by email", e);
            throw new KeycloakOperationException("Failed to search for user", e);
        }
    }

    private UserRepresentation buildUserRepresentation(RegisterRequest request) {

        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEnabled(true);
        user.setEmailVerified(false);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.password());
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));

        return user;
    }

    private void sendVerifyEmailSafely(UserResource userResource) {
        try {
            userResource.sendVerifyEmail();
        } catch (RuntimeException e) {
            // Don't fail the whole request just because the email didn't send —
            // log it and let the user request a resend later.
            log.error("Failed to send verification email", e);
        }
    }

    private void assignRole(UserResource userResource, RoleEnum role) {

        RolesResource rolesResource = keycloak.realm(props.getTargetRealm()).roles();

        RoleRepresentation roleRepresentation;
        try {
            roleRepresentation = rolesResource.get(role.name()).toRepresentation();
        } catch (RuntimeException e) {
            log.error("Role {} not found in realm", role.name(), e);
            throw new KeycloakOperationException("Required role not configured: " + role.name(), e);
        }

        try {
            userResource.roles().realmLevel().add(List.of(roleRepresentation));
        } catch (RuntimeException e) {
            log.error("Failed to assign role {} to user", role.name(), e);
            throw new KeycloakOperationException("Failed to assign role to user", e);
        }
    }

    private String safeReadBody(Response response) {
        try {
            return response.readEntity(String.class);
        } catch (RuntimeException e) {
            return "<unreadable>";
        }
    }

    private TokenResponse requestToken(MultiValueMap<String, String> form) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String url = String.format(TOKEN_ENDPOINT, props.getServerUrl(), props.getTargetRealm());

        try {
            ResponseEntity<KeycloakTokenResponse> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(form, headers), KeycloakTokenResponse.class);

            KeycloakTokenResponse body = response.getBody();
            if (body == null || body.accessToken() == null) {
                log.warn("Token response missing access token");
                throw new InvalidCredentialsException();
            }

            return TokenResponse.builder()
                    .accessToken(body.accessToken())
                    .refreshToken(body.refreshToken())
                    .tokenType(body.tokenType())
                    .expiresIn(body.expiresIn())
                    .refreshExpiresIn(body.refreshExpiresIn())
                    .build();

        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.BadRequest e) {
            log.warn("Token request rejected: {}", e.getResponseBodyAsString());
            throw new InvalidCredentialsException();
        } catch (RestClientException e) {
            log.error("Token request failed to reach Keycloak", e);
            throw new KeycloakOperationException("Failed to reach identity provider", e);
        }
    }
}