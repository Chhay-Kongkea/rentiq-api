package co.istad.rentiq_api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
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
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        /*
                         * =====================================================
                         * API DOCUMENTATION
                         * =====================================================
                         */
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

                        /*
                         * =====================================================
                         * AUTHENTICATION
                         * =====================================================
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/register"
                        )
                        .permitAll()

                        /*
                         * =====================================================
                         * CATEGORIES
                         * =====================================================
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/categories",
                                "/api/v1/categories/**"
                        )
                        .permitAll()

                        /*
                         * =====================================================
                         * PUBLIC ITEMS
                         * =====================================================
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/items",
                                "/api/v1/items/featured",
                                "/api/v1/items/nearby",
                                "/api/v1/items/*",
                                "/api/v1/items/*/images",
                                "/api/v1/items/*/reviews",
                                "/api/v1/items/*/availability",
                                "/api/v1/vendors/*/items"
                        )
                        .permitAll()

                        /*
                         * =====================================================
                         * SEARCH AND DISCOVERY
                         * =====================================================
                         */
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

                        /*
                         * =====================================================
                         * PUBLIC ITEM REQUESTS
                         * =====================================================
                         */

                        /*
                         * Must be declared before:
                         * /api/v1/item-requests/*
                         */
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

                        /*
                         * =====================================================
                         * USER ITEM REQUEST MANAGEMENT
                         * =====================================================
                         */
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
                                "/api/v1/users/me/item-requests",
                                "/api/v1/users/me/item-requests/**"
                        )
                        .hasRole("USER")

                        /*
                         * =====================================================
                         * VENDOR ITEM MANAGEMENT
                         * =====================================================
                         */
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

                        /*
                         * =====================================================
                         * OFFER MANAGEMENT
                         * =====================================================
                         */

                        /*
                         * Vendor submits an offer.
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/item-requests/*/offers"
                        )
                        .hasRole("VENDOR")

                        /*
                         * Customer views offers received for a request.
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/item-requests/*/offers"
                        )
                        .hasRole("USER")

                        /*
                         * Customer accepts or rejects an offer.
                         */
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/item-requests/*/offers/*/accept",
                                "/api/v1/item-requests/*/offers/*/reject"
                        )
                        .hasRole("USER")

                        /*
                         * Vendor reads, updates, or withdraws own offer.
                         */
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

                        /*
                         * Vendor tracks submitted offers.
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/vendors/me/offers",
                                "/api/v1/vendors/me/offers/*/status"
                        )
                        .hasRole("VENDOR")

                        /*
                         * =====================================================
                         * USER NOTIFICATIONS
                         * =====================================================
                         */
                        .requestMatchers(
                                "/api/v1/notifications",
                                "/api/v1/notifications/**"
                        )
                        .hasRole("USER")

                        /*
                         * =====================================================
                         * USER REPORTS
                         * =====================================================
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/reports"
                        )
                        .hasRole("USER")

                        /*
                         * Declare /me before /{reportId}.
                         */
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

                        /*
                         * =====================================================
                         * ADMIN ITEM MANAGEMENT
                         * =====================================================
                         */
                        .requestMatchers(
                                "/api/v1/admin/items",
                                "/api/v1/admin/items/**"
                        )
                        .hasRole("ADMIN")

                        /*
                         * =====================================================
                         * ADMIN NOTIFICATIONS
                         * =====================================================
                         */
                        .requestMatchers(
                                "/api/v1/admin/notifications",
                                "/api/v1/admin/notifications/**"
                        )
                        .hasRole("ADMIN")

                        /*
                         * =====================================================
                         * ADMIN REPORTS
                         * =====================================================
                         */
                        .requestMatchers(
                                "/api/v1/admin/reports",
                                "/api/v1/admin/reports/**"
                        )
                        .hasRole("ADMIN")

                        /*
                         * =====================================================
                         * FALLBACK
                         * Must always be the final authorization rule.
                         * =====================================================
                         */
                        .anyRequest()
                        .authenticated()
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
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
}