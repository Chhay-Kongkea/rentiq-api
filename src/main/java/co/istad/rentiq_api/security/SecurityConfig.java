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
                .csrf(AbstractHttpConfigurer::disable)

                .cors(Customizer.withDefaults())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                )

                .authorizeHttpRequests(auth -> auth

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

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/auth/verify-email"
                        )
                        .permitAll()


                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/logout",
                                "/api/v1/auth/change-password"
                        )
                        .authenticated()


                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/categories",
                                "/api/v1/categories/**"
                        )
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/category/**"
                        )
                        .permitAll()


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


                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/search/items",
                                "/api/v1/search/suggestions",
                                "/api/v1/search/nearby"
                        )
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/search/logs"
                        )
                        .hasRole("USER")

                        .requestMatchers(
                                "/api/v1/admin/search-logs",
                                "/api/v1/admin/search-logs/**"
                        )
                        .hasRole("ADMIN")


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


                        .requestMatchers(
                                "/api/v1/users/me",
                                "/api/v1/users/me/**"
                        )
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/users/*"
                        )
                        .permitAll()


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

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/bookings/qr-code/scan"
                        )
                        .hasRole("VENDOR")

                        .requestMatchers(
                                "/api/v1/admin/bookings",
                                "/api/v1/admin/bookings/**"
                        )
                        .hasRole("ADMIN")


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

                        .requestMatchers(
                                "/api/v1/admin/disputes",
                                "/api/v1/admin/disputes/**"
                        )
                        .hasRole("ADMIN")


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


                        .requestMatchers(
                                "/api/v1/notifications",
                                "/api/v1/notifications/**"
                        )
                        .hasRole("USER")


                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/reports"
                        )
                        .hasRole("USER")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/reports/me"
                        )
                        .hasRole("USER")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/reports/*"
                        )
                        .hasRole("USER")


                        .requestMatchers(
                                "/api/v1/admin/items",
                                "/api/v1/admin/items/**"
                        )
                        .hasRole("ADMIN")

                        .requestMatchers(
                                "/api/v1/admin/kyc",
                                "/api/v1/admin/kyc/**"
                        )
                        .hasRole("ADMIN")

                        .requestMatchers(
                                "/api/v1/admin/notifications",
                                "/api/v1/admin/notifications/**"
                        )
                        .hasRole("ADMIN")

                        .requestMatchers(
                                "/api/v1/admin/reports",
                                "/api/v1/admin/reports/**"
                        )
                        .hasRole("ADMIN")


                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/wallets/topup-requests/webhook"
                        )
                        .permitAll()

                        .requestMatchers(
                                "/api/v1/wallets/me",
                                "/api/v1/wallets/me/**"
                        )
                        .hasRole("VENDOR")

                        .requestMatchers(
                                "/api/v1/admin/wallets",
                                "/api/v1/admin/wallets/**"
                        )
                        .hasRole("ADMIN")

                        .requestMatchers(
                                "/api/v1/admin/topup-requests",
                                "/api/v1/admin/topup-requests/**"
                        )
                        .hasRole("ADMIN")

                        .requestMatchers(
                                "/api/v1/admin/categories",
                                "/api/v1/admin/categories/**"
                        )
                        .hasRole("ADMIN")

                        .requestMatchers(
                                "/api/v1/admin/commissions",
                                "/api/v1/admin/commissions/**"
                        )
                        .hasRole("ADMIN")

                        .requestMatchers(
                                "/api/v1/vendors/me",
                                "/api/v1/vendors/me/**"
                        )
                        .hasRole("VENDOR")

                        .requestMatchers(
                                "/api/v1/admin/vendors",
                                "/api/v1/admin/vendors/**"
                        )
                        .hasRole("ADMIN")

                        .anyRequest()
                        .authenticated()
                )
                .oauth2Login(oauth2 -> oauth2

                        .authorizationEndpoint(endpoint ->
                                endpoint.authorizationRequestResolver(
                                        authorizationRequestResolver
                                )
                        )

                        .successHandler((request, response, authentication) ->
                                response.sendRedirect(
                                        "http://localhost:8081/auth/callback"
                                )
                        )

                        .failureHandler((request, response, exception) ->
                                response.sendRedirect(
                                        "http://localhost:8081/login"
                                                + "?error=keycloak_login_failed"
                                )
                        )
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
                )
                .addFilterAfter(
                        bannedAccountFilter,
                        BearerTokenAuthenticationFilter.class
                );

        return http.build();
    }

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

            if (!(rolesValue
                    instanceof Collection<?> roles)) {
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


        @Bean
        public OAuth2AuthorizedClientService oAuth2AuthorizedClientService(
                ClientRegistrationRepository clientRegistrationRepository
        ) {
                return new InMemoryOAuth2AuthorizedClientService(
                        clientRegistrationRepository
                );
        }
}