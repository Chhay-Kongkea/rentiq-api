//package co.istad.rentiq_api.security;
//
//
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpMethod;
//import org.springframework.security.config.Customizer;
//import org.springframework.core.convert.converter.Converter;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.oauth2.jwt.Jwt;
//import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
//import org.springframework.security.web.SecurityFilterChain;
//
//import java.util.Collection;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Configuration
//public class SecurityConfig {
//
//
//    private static final String[] PUBLIC_ENDPOINTS = {
//            "/api/v1/auth/register",
//            "/api/v1/auth/login",
//            "/api/v1/auth/refresh-token",
//            "/api/v1/auth/forgot-password",
//            "/api/v1/auth/verify-email",
//            "/api/v1/auth/resend-verification-email"
//    };
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//
//        http
//                .csrf(AbstractHttpConfigurer::disable)
//                .sessionManagement(session ->
//                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//                )
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/v3/api-docs/**",
//                                "/swagger-ui/**","/swagger-ui.html").permitAll()
//                        .requestMatchers(
//                                "/scalar",
//                                "/scalar/**",
//                                "/v3/api-docs",
//                                "/v3/api-docs/**"
//                        )
//
//                        .permitAll()
//
//
//                        .requestMatchers(HttpMethod.GET,
//                                "/api/v1/**","/api/v1/categories/**").permitAll()
//                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
//                        .requestMatchers(HttpMethod.POST, "/api/v1/category/**").permitAll()
//
//                                .requestMatchers(
//                                        HttpMethod.GET,
//                                        "/api/v1/items",
//                                        "/api/v1/items/featured",
//                                        "/api/v1/items/nearby",
//                                        "/api/v1/items/*",
//                                        "/api/v1/items/*/images",
//                                        "/api/v1/items/*/reviews",
//                                        "/api/v1/items/*/availability",
//                                        "/api/v1/vendors/*/items"
//                                )
//                                .permitAll()
//
//                                .requestMatchers(
//                                        HttpMethod.POST,
//                                        "/api/v1/items",
//                                        "/api/v1/items/*/images",
//                                        "/api/v1/items/*/availability-block"
//                                )
//                                .hasRole("VENDOR")
//
//                                .requestMatchers(
//                                        HttpMethod.PATCH,
//                                        "/api/v1/items/*",
//                                        "/api/v1/items/*/availability",
//                                        "/api/v1/items/*/status",
//                                        "/api/v1/items/*/images/*"
//                                )
//                                .hasRole("VENDOR")
//
//                                .requestMatchers(
//                                        HttpMethod.DELETE,
//                                        "/api/v1/items/*",
//                                        "/api/v1/items/*/images/*",
//                                        "/api/v1/items/*/availability-block/*"
//                                )
//                                .hasRole("VENDOR")
//
//                                .requestMatchers(
//                                        "/api/v1/vendors/me/items",
//                                        "/api/v1/vendors/me/items/**"
//                                )
//                                .hasRole("VENDOR")
//
//                             .requestMatchers(
//                                HttpMethod.GET,
//                                  "/api/v1/items/*/images"
//                             )
//                             .permitAll()
//
//                            .requestMatchers(
//                                    HttpMethod.POST,
//                                    "/api/v1/items/*/images"
//                            )
//                            .hasRole("VENDOR")
//
//                            .requestMatchers(
//                                    HttpMethod.PATCH,
//                                    "/api/v1/items/*/images/*"
//                            )
//                            .hasRole("VENDOR")
//
//                            .requestMatchers(
//                                    HttpMethod.DELETE,
//                                    "/api/v1/items/*/images/*"
//                            )
//                            .hasRole("VENDOR")
//                              .requestMatchers(
//                                            "/api/v1/admin/items",
//                                            "/api/v1/admin/items/**"
//                                    )
//                                    .hasRole("ADMIN")
//                            .anyRequest().authenticated()
//                )
//                .oauth2ResourceServer(oauth2 ->
//                        oauth2.jwt(Customizer.withDefaults())
//                );
//
//        return http.build();
//    }
//
//    @Bean
//    public JwtAuthenticationConverter jwtAuthenticationConverterForKeycloak() {
//        Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter = jwt -> {
//            Map<String, Collection<String>> realmAccess = jwt.getClaim("realm_access");
//            Collection<String> roles = realmAccess.get("roles");
//            return roles.stream()
//                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
//                    .collect(Collectors.toList());
//        };
//
//        var jwtAuthenticationConverter = new JwtAuthenticationConverter();
//        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
//
//        return jwtAuthenticationConverter;
//    }
//
//}



package co.istad.rentiq_api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_GET_ENDPOINTS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/scalar",
            "/scalar/**"
    };

    private static final String[] PUBLIC_AUTH_ENDPOINTS = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/resend-verification-email"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth

                        // --- Docs / infra ---
                        .requestMatchers(PUBLIC_GET_ENDPOINTS).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/bookings/*/disputes"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/bookings/*/disputes",
                                "/api/v1/disputes/*"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/disputes/*"
                        ).authenticated()
                        .requestMatchers(HttpMethod.POST, PUBLIC_AUTH_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/verify-email").permitAll()



                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/categories/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/category/**").permitAll()


                                // Customer create booking
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/bookings"
                                )
                                .authenticated()


                                // Customer booking access
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


                                // Update booking status
                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/v1/bookings/*/status"
                                )
                                .authenticated()


                                // Vendor scan QR
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/bookings/qr-code/scan"
                                )
                                .hasRole("VENDOR")


                                // Admin booking management
                                .requestMatchers(
                                        "/api/v1/admin/bookings",
                                        "/api/v1/admin/bookings/**"
                                )
                                .hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/items",
                                "/api/v1/items/featured",
                                "/api/v1/items/nearby",
                                "/api/v1/items/*",
                                "/api/v1/items/*/images",
                                "/api/v1/items/*/reviews",
                                "/api/v1/bookings/*/reviews",
                                "/api/v1/items/*/availability",
                                "/api/v1/vendors/*/items"
                        ).permitAll()


                        // USER PROFILE
                        .requestMatchers(
                                "/api/v1/users/me",
                                "/api/v1/users/me/**"
                        )
                        .authenticated()






                        // --- User KYC ---
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/kyc"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/kyc/me"
                        ).authenticated()


                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/bookings/*/inspections",
                                "/api/v1/bookings/*/inspections/images"
                        ).hasRole("VENDOR")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/bookings/*/inspections"
                        ).hasRole("VENDOR")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/bookings/*/inspections/images/*"
                        ).hasRole("VENDOR")


                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/bookings/*/inspections",
                                "/api/v1/bookings/*/inspections/images"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/kyc/me"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/kyc/me/verify-email",
                                "/api/v1/kyc/me/verify-email/confirm"
                        ).authenticated()

                        // --- Vendor-only write operations ---
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/items",
                                "/api/v1/items/*/images",
                                "/api/v1/items/*/availability-block"
                        ).hasRole("VENDOR")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/items/*",
                                "/api/v1/items/*/availability",
                                "/api/v1/items/*/status",
                                "/api/v1/items/*/images/*"
                        ).hasRole("VENDOR")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/items/*",
                                "/api/v1/items/*/images/*",
                                "/api/v1/items/*/availability-block/*"
                        ).hasRole("VENDOR")

                        .requestMatchers(
                                "/api/v1/vendors/me/items",
                                "/api/v1/vendors/me/items/**"
                        ).hasRole("VENDOR")

                        // --- Admin-only ---
                        .requestMatchers(
                                "/api/v1/admin/items",
                                "/api/v1/admin/items/**",
                                "/api/v1/admin/kyc",
                                "/api/v1/admin/kyc/**",
                                "/api/v1/admin/disputes",
                                "/api/v1/admin/disputes/**"
                        ).hasRole("ADMIN")
                        .requestMatchers(
                                "/api/v1/admin/items",
                                "/api/v1/admin/items/**",
                                "/api/v1/admin/kyc",
                                "/api/v1/admin/kyc/**"
                        ).hasRole("ADMIN")

                        // --- Everything else needs a valid token ---
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtAuthenticationConverterForKeycloak()
                                )
                        )
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverterForKeycloak() {

        Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter = jwt -> {

            Map<String, Object> realmAccess = jwt.getClaim("realm_access");

            if (realmAccess == null || realmAccess.get("roles") == null) {
                return Collections.emptyList();
            }

            @SuppressWarnings("unchecked")
            Collection<String> roles = (Collection<String>) realmAccess.get("roles");

            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        };

        var jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }
}