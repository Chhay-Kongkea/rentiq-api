package co.istad.rentiq_api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(
                HttpSecurity http,
                JwtAuthenticationConverter jwtAuthenticationConverter,
                ClientRegistrationRepository clientRegistrationRepository,
                BannedAccountFilter bannedAccountFilter
        ) throws Exception {

                DefaultOAuth2AuthorizationRequestResolver authorizationRequestResolver =
                        new DefaultOAuth2AuthorizationRequestResolver(
                                clientRegistrationRepository,
                                "/oauth2/authorization"
                        );

                authorizationRequestResolver.setAuthorizationRequestCustomizer(
                        OAuth2AuthorizationRequestCustomizers.withPkce()
                );

                http

                        // ============================================================
                        // BASIC CONFIG
                        // ============================================================

                        .csrf(AbstractHttpConfigurer::disable)

                        .cors(Customizer.withDefaults())

                        .sessionManagement(session ->
                                session.sessionCreationPolicy(
                                        SessionCreationPolicy.IF_REQUIRED
                                )
                        )

                        // ============================================================
                        // AUTHORIZATION
                        // ============================================================

                        .authorizeHttpRequests(auth -> auth

                                // ====================================================
                                // SWAGGER / API DOCUMENTATION
                                // ====================================================

                                .requestMatchers(
                                        "/v3/api-docs",
                                        "/v3/api-docs/**",
                                        "/swagger-ui.html",
                                        "/swagger-ui/**",
                                        "/scalar",
                                        "/scalar/**",
                                        "/scalar.html"
                                )
                                .permitAll()


                                // ====================================================
                                // AUTH - PUBLIC
                                // ====================================================

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/auth/register",
                                        "/api/v1/auth/user/login",
                                        "/api/v1/auth/login",
                                        "/api/v1/auth/resend-verification-email",
                                        "/api/v1/auth/forgot-password",
                                        "/api/v1/auth/refresh-token"
                                )
                                .permitAll()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/auth/login",
                                        "/api/v1/auth/verify-email"
                                )
                                .permitAll()

                                .requestMatchers(
                                        "/oauth2/**",
                                        "/login/**",
                                        "/error"
                                )
                                .permitAll()


                                // ====================================================
                                // AUTH - AUTHENTICATED
                                // ====================================================

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/auth/logout",
                                        "/api/v1/auth/change-password"
                                )
                                .authenticated()


                                // ====================================================
                                // CATEGORIES - PUBLIC READ
                                // ====================================================

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/categories",
                                        "/api/v1/categories/**"
                                )
                                .permitAll()


                                // ====================================================
                                // GLOBAL IMAGE UPLOAD
                                // ====================================================

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/images/upload"
                                )
                                .hasAnyRole(
                                        "USER",
                                        "VENDOR",
                                        "ADMIN"
                                )

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/images/**"
                                )
                                .hasAnyRole(
                                        "USER",
                                        "VENDOR",
                                        "ADMIN"
                                )

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/v1/images/**"
                                )
                                .hasAnyRole(
                                        "USER",
                                        "VENDOR",
                                        "ADMIN"
                                )


                                // ====================================================
                                // ITEMS - PUBLIC READ
                                // ====================================================

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/items",
                                        "/api/v1/items/featured",
                                        "/api/v1/items/nearby",
                                        "/api/v1/items/*",
                                        "/api/v1/items/*/images",
                                        "/api/v1/items/*/reviews",
                                        "/api/v1/items/*/availability",
                                        "/api/v1/vendors/*/items",
                                        "/api/v1/bookings/*/reviews"
                                )
                                .permitAll()


                                // ====================================================
                                // ITEMS - VENDOR
                                // ====================================================

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/items",
                                        "/api/v1/items/*/images",
                                        "/api/v1/items/*/availability-block"
                                )
                                .hasRole("VENDOR")

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/v1/items/*",
                                        "/api/v1/items/*/availability",
                                        "/api/v1/items/*/status",
                                        "/api/v1/items/*/images/*"
                                )
                                .hasRole("VENDOR")

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/v1/items/*",
                                        "/api/v1/items/*/images/*",
                                        "/api/v1/items/*/availability-block/*"
                                )
                                .hasRole("VENDOR")

                                .requestMatchers(
                                        "/api/v1/vendors/me/items",
                                        "/api/v1/vendors/me/items/**"
                                )
                                .hasRole("VENDOR")


                                // ====================================================
                                // SEARCH - PUBLIC
                                // ====================================================

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/search/items",
                                        "/api/v1/search/suggestions",
                                        "/api/v1/search/nearby"
                                )
                                .permitAll()


                                // ====================================================
                                // SEARCH LOGS - USER
                                // ====================================================

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/search/logs"
                                )
                                .hasRole("USER")


                                // ====================================================
                                // ITEM REQUESTS
                                // ====================================================

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/item-requests/nearby"
                                )
                                .hasRole("VENDOR")

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/item-requests"
                                )
                                .permitAll()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/item-requests/*"
                                )
                                .hasRole("USER")

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/item-requests"
                                )
                                .hasRole("USER")

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/v1/item-requests/*"
                                )
                                .hasRole("USER")

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/v1/item-requests/*"
                                )
                                .hasRole("USER")

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/users/me/item-requests"
                                )
                                .hasRole("USER")


                                // ====================================================
                                // OFFERS
                                // ====================================================

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/item-requests/*/offers"
                                )
                                .hasRole("VENDOR")

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/item-requests/*/offers"
                                )
                                .hasRole("USER")

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/v1/item-requests/*/offers/*/accept",
                                        "/api/v1/item-requests/*/offers/*/reject"
                                )
                                .hasRole("USER")

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/offers/*"
                                )
                                .hasRole("VENDOR")

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/v1/offers/*"
                                )
                                .hasRole("VENDOR")

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/v1/offers/*"
                                )
                                .hasRole("VENDOR")

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/vendors/me/offers",
                                        "/api/v1/vendors/me/offers/*/status"
                                )
                                .hasRole("VENDOR")


                                // ====================================================
                                // REVIEW - VENDOR REPLY
                                // ====================================================

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/reviews/*/reply"
                                )
                                .hasRole("VENDOR")

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/v1/reviews/*/reply"
                                )
                                .hasRole("VENDOR")


                                // ====================================================
                                // REVIEW IMAGES
                                // ====================================================

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/reviews/*/images"
                                )
                                .authenticated()

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/v1/reviews/*/images/*"
                                )
                                .authenticated()


                                // ====================================================
                                // REVIEWS
                                // ====================================================

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/reviews/*"
                                )
                                .permitAll()

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/v1/reviews/*"
                                )
                                .authenticated()

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/v1/reviews/*"
                                )
                                .authenticated()

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/bookings/*/review"
                                )
                                .authenticated()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/users/me/reviews"
                                )
                                .authenticated()


                                // ====================================================
                                // USER PROFILE
                                // ====================================================

                                .requestMatchers(
                                        "/api/v1/users/me",
                                        "/api/v1/users/me/**"
                                )
                                .authenticated()

                                /*
                                 * Public user profile.
                                 *
                                 * IMPORTANT:
                                 * Make sure this endpoint does not return:
                                 * - phone number
                                 * - KYC information
                                 * - national ID
                                 * - private email
                                 */
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/users/*"
                                )
                                .permitAll()


                                // ====================================================
                                // KYC
                                // ====================================================

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/kyc"
                                )
                                .authenticated()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/kyc/me"
                                )
                                .authenticated()

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/v1/kyc/me"
                                )
                                .authenticated()

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/kyc/me/verify-email",
                                        "/api/v1/kyc/me/verify-email/confirm"
                                )
                                .authenticated()


                                // ====================================================
                                // BOOKINGS
                                // ====================================================

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/bookings"
                                )
                                .authenticated()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/bookings",
                                        "/api/v1/bookings/*",
                                        "/api/v1/bookings/*/status-history",
                                        "/api/v1/bookings/*/qr-code",
                                        "/api/v1/bookings/*/receipt",
                                        "/api/v1/bookings/*/invoice"
                                )
                                .authenticated()

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/v1/bookings/*/status"
                                )
                                .authenticated()


                                // ====================================================
                                // QR CODE
                                // ====================================================

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/bookings/qr-code/scan"
                                )
                                .hasRole("VENDOR")


                                // ====================================================
                                // DISPUTES
                                // ====================================================

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/bookings/*/disputes"
                                )
                                .authenticated()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/bookings/*/disputes",
                                        "/api/v1/disputes/*"
                                )
                                .authenticated()

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/v1/disputes/*"
                                )
                                .authenticated()


                                // ====================================================
                                // INSPECTIONS
                                // ====================================================

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/bookings/*/inspections",
                                        "/api/v1/bookings/*/inspections/images"
                                )
                                .hasRole("VENDOR")

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/v1/bookings/*/inspections"
                                )
                                .hasRole("VENDOR")

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/v1/bookings/*/inspections/images/*"
                                )
                                .hasRole("VENDOR")

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/bookings/*/inspections",
                                        "/api/v1/bookings/*/inspections/images"
                                )
                                .authenticated()


                                // ====================================================
                                // NOTIFICATIONS
                                // USER + VENDOR
                                // ====================================================

                                .requestMatchers(
                                        "/api/v1/notifications",
                                        "/api/v1/notifications/**"
                                )
                                .hasAnyRole(
                                        "USER",
                                        "VENDOR"
                                )


                                // ====================================================
                                // REPORTS
                                // USER + VENDOR
                                // ====================================================

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/reports"
                                )
                                .hasAnyRole(
                                        "USER",
                                        "VENDOR"
                                )

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/reports/me"
                                )
                                .hasAnyRole(
                                        "USER",
                                        "VENDOR"
                                )

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/reports/*"
                                )
                                .hasAnyRole(
                                        "USER",
                                        "VENDOR"
                                )


                                // ====================================================
                                // WALLET WEBHOOK
                                //
                                // Keep public because external payment provider
                                // will normally not have a Keycloak access token.
                                //
                                // IMPORTANT:
                                // Validate webhook signature inside controller/service.
                                // ====================================================

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/wallets/topup-requests/webhook"
                                )
                                .permitAll()


                                // ====================================================
                                // VENDOR WALLET
                                // ====================================================

                                .requestMatchers(
                                        "/api/v1/wallets/me",
                                        "/api/v1/wallets/me/**"
                                )
                                .hasRole("VENDOR")


                                // ====================================================
                                // VENDOR PROFILE
                                // ====================================================

                                .requestMatchers(
                                        "/api/v1/vendors/me",
                                        "/api/v1/vendors/me/**"
                                )
                                .hasRole("VENDOR")


                                // ====================================================
                                // ALL ADMIN ENDPOINTS
                                //
                                // This MUST stay before anyRequest().
                                //
                                // Covers:
                                // /api/v1/admin/users/**
                                // /api/v1/admin/vendors/**
                                // /api/v1/admin/items/**
                                // /api/v1/admin/bookings/**
                                // /api/v1/admin/kyc/**
                                // /api/v1/admin/reports/**
                                // /api/v1/admin/wallets/**
                                // etc.
                                // ====================================================

                                .requestMatchers(
                                        "/api/v1/admin/**"
                                )
                                .hasRole("ADMIN")


                                // ====================================================
                                // FALLBACK
                                //
                                // Any endpoint not listed above requires login.
                                // ====================================================

                                .anyRequest()
                                .authenticated()
                        )


                        // ============================================================
                        // OAUTH2 LOGIN
                        // ============================================================

                        .oauth2Login(oauth2 -> oauth2

                                .authorizationEndpoint(endpoint ->
                                        endpoint.authorizationRequestResolver(
                                                authorizationRequestResolver
                                        )
                                )

                                .successHandler(
                                        (request, response, authentication) ->
                                                response.sendRedirect(
                                                        "http://localhost:8081/auth/callback"
                                                )
                                )

                                .failureHandler(
                                        (request, response, exception) ->
                                                response.sendRedirect(
                                                        "http://localhost:8081/login"
                                                                + "?error=keycloak_login_failed"
                                                )
                                )
                        )


                        // ============================================================
                        // JWT RESOURCE SERVER
                        // ============================================================

                        .oauth2ResourceServer(oauth2 ->
                                oauth2.jwt(jwt ->
                                        jwt.jwtAuthenticationConverter(
                                                jwtAuthenticationConverter
                                        )
                                )
                        )


                        // ============================================================
                        // SUSPENDED / BANNED ACCOUNT FILTER
                        // ============================================================

                        .addFilterAfter(
                                bannedAccountFilter,
                                BearerTokenAuthenticationFilter.class
                        );

                return http.build();
        }


        // ================================================================
        // KEYCLOAK ROLE CONVERTER
        //
        // Keycloak:
        // USER
        // VENDOR
        // ADMIN
        //
        // Spring:
        // ROLE_USER
        // ROLE_VENDOR
        // ROLE_ADMIN
        // ================================================================

        @Bean
        public JwtAuthenticationConverter
        jwtAuthenticationConverterForKeycloak() {

                Converter<Jwt, Collection<GrantedAuthority>>
                        authoritiesConverter = jwt -> {

                        Map<String, Object> realmAccess =
                                jwt.getClaimAsMap("realm_access");

                        if (realmAccess == null) {
                                return Collections.emptyList();
                        }

                        Object rolesValue =
                                realmAccess.get("roles");

                        if (!(rolesValue instanceof Collection<?> roles)) {
                                return Collections.emptyList();
                        }

                        return roles.stream()
                                .filter(String.class::isInstance)
                                .map(String.class::cast)
                                .map(String::trim)
                                .filter(role -> !role.isBlank())
                                .map(String::toUpperCase)
                                .map(role ->
                                        new SimpleGrantedAuthority(
                                                "ROLE_" + role
                                        )
                                )
                                .collect(Collectors.toList());
                };

                JwtAuthenticationConverter converter =
                        new JwtAuthenticationConverter();

                converter.setJwtGrantedAuthoritiesConverter(
                        authoritiesConverter
                );

                return converter;
        }


        // ================================================================
        // OAUTH2 AUTHORIZED CLIENT SERVICE
        // ================================================================

        @Bean
        public OAuth2AuthorizedClientService oAuth2AuthorizedClientService(
                ClientRegistrationRepository clientRegistrationRepository
        ) {

                return new InMemoryOAuth2AuthorizedClientService(
                        clientRegistrationRepository
                );
        }
}