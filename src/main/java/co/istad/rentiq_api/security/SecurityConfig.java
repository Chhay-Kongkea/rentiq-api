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
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        /*
                         * API documentation
                         */
                        .requestMatchers(
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/scalar",
                                "/scalar/**"
                        )
                        .permitAll()

                        /*
                         * Authentication
                         */
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register")
                        .permitAll()

                        /*
                         * Categories
                         */
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories", "/api/v1/categories/**")
                        .permitAll()

                        /*
                         * Public items
                         */
                        .requestMatchers(HttpMethod.GET,
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
                         * Vendor item management
                         */
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/items",
                                "/api/v1/items/*/images",
                                "/api/v1/items/*/availability-block"
                        )
                        .hasRole("VENDOR")

                        .requestMatchers(HttpMethod.PATCH,
                                "/api/v1/items/*",
                                "/api/v1/items/*/availability",
                                "/api/v1/items/*/status",
                                "/api/v1/items/*/images/*"
                        )
                        .hasRole("VENDOR")

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/v1/items/*",
                                "/api/v1/items/*/images/*",
                                "/api/v1/items/*/availability-block/*"
                        )
                        .hasRole("VENDOR")

                        .requestMatchers("/api/v1/vendors/me/items", "/api/v1/vendors/me/items/**")
                        .hasRole("VENDOR")

                        /*
                         * Admin item management
                         */
                        .requestMatchers("/api/v1/admin/items", "/api/v1/admin/items/**")
                        .hasRole("ADMIN")

                        /*
                         * Search and discovery
                         */
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/search/items",
                                "/api/v1/search/suggestions",
                                "/api/v1/search/nearby"
                        )
                        .permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/v1/search/logs")
                        .hasRole("USER")

                        .requestMatchers("/api/v1/admin/search-logs", "/api/v1/admin/search-logs/**")
                        .hasRole("ADMIN")

                        /*
                         * Item request nearby must be declared
                         * before /item-requests/*.
                         */
                        .requestMatchers(HttpMethod.GET, "/api/v1/item-requests/nearby")
                        .hasRole("VENDOR")

                        /*
                         * Public item-request listing
                         */
                        .requestMatchers(HttpMethod.GET, "/api/v1/item-requests")
                        .permitAll()

                        /*
                         * Request detail according to your endpoint table:
                         * USER access.
                         */
                        .requestMatchers(HttpMethod.GET, "/api/v1/item-requests/*")
                        .hasRole("USER")

                        /*
                         * Customer request management
                         */
                        .requestMatchers(HttpMethod.POST, "/api/v1/item-requests")
                        .hasRole("USER")

                        .requestMatchers(HttpMethod.PATCH, "/api/v1/item-requests/*")
                        .hasRole("USER")

                        .requestMatchers(HttpMethod.DELETE, "/api/v1/item-requests/*")
                        .hasRole("USER")

                        .requestMatchers("/api/v1/users/me/item-requests", "/api/v1/users/me/item-requests/**")
                        .hasRole("USER")

                        /*
                         * Vendor sends an offer
                         */
                        .requestMatchers(HttpMethod.POST, "/api/v1/item-requests/*/offers")
                        .hasRole("VENDOR")

                        /*
                         * Customer views and manages received offers
                         */
                        .requestMatchers(HttpMethod.GET, "/api/v1/item-requests/*/offers")
                        .hasRole("USER")

                        .requestMatchers(HttpMethod.PATCH,
                                "/api/v1/item-requests/*/offers/*/accept",
                                "/api/v1/item-requests/*/offers/*/reject"
                        )
                        .hasRole("USER")

                        /*
                         * Vendor offer endpoints from your official API:
                         */
                        .requestMatchers(HttpMethod.GET, "/api/v1/offers/*")
                        .hasRole("VENDOR")

                        .requestMatchers(HttpMethod.PATCH, "/api/v1/offers/*")
                        .hasRole("VENDOR")

                        .requestMatchers(HttpMethod.DELETE, "/api/v1/offers/*")
                        .hasRole("VENDOR")

                        /*
                         * Vendor tracks offers
                         */
                        .requestMatchers(HttpMethod.GET, "/api/v1/vendors/me/offers", "/api/v1/vendors/me/offers/*/status")
                        .hasRole("VENDOR")

                        /*
                         * All unmatched endpoints require authentication.
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
    public JwtAuthenticationConverter jwtAuthenticationConverterForKeycloak() {

        Converter<Jwt, Collection<GrantedAuthority>> authoritiesConverter = jwt -> {

            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

            if (realmAccess == null) {
                return Collections.emptyList();
            }

            Object rolesValue = realmAccess.get("roles");

            if (!(rolesValue instanceof Collection<?> roles)) {
                return Collections.emptyList();
            }

            return roles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(String::toUpperCase)
                    .map(role ->
                            new SimpleGrantedAuthority("ROLE_" + role)
                    )
                    .collect(Collectors.toList());
        };

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return converter;
    }
}