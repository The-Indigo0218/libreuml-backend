package com.libreuml.backend.infrastructure.security.config;

import com.libreuml.backend.infrastructure.security.ApiKeyAuthenticationFilter;
import com.libreuml.backend.infrastructure.security.CustomUserDetailsService;
import com.libreuml.backend.infrastructure.security.JwtAuthenticationFilter;
import com.libreuml.backend.infrastructure.security.JwtCookieAuthFilter;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.AuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtCookieAuthFilter jwtCookieAuthFilter;
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoderConfig passwordEncoderConfig;

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/oauth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // Actuator: liveness/readiness probes are public for Kubernetes health checks.
                        // Prometheus scraping requires ADMIN to prevent metric reconnaissance.
                        .requestMatchers("/internal/health/**").permitAll()
                        .requestMatchers("/internal/prometheus").hasRole("ADMIN")
                        // OpenAPI / Swagger UI: documentation endpoints are public (no sensitive data).
                        // springdoc serves the UI at /api/docs and the JSON spec at /api/api-docs.
                        .requestMatchers("/api/docs/**", "/api/docs.html").permitAll()
                        .requestMatchers("/api/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedEntryPoint()))
                .authenticationProvider(authenticationProvider())
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(63072000)
                        )
                        .cacheControl(Customizer.withDefaults())
                        .addHeaderWriter((req, res) -> {
                            res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
                            res.setHeader("Permissions-Policy", "geolocation=(), camera=(), microphone=()");
                            res.setHeader("Cross-Origin-Opener-Policy", "same-origin");
                            res.setHeader("Cross-Origin-Resource-Policy", "same-site");
                        })
                )
                // Both filters are registered at order 799 (one slot before UPAF = 800).
                // Filters at the same order run in insertion order — apiKey is added first,
                // so it executes before jwtCookie. Each filter checks for its own header prefix
                // and passes through when the other transport is in use.
                .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtCookieAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, ex) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication required.\",\"path\":\"" + request.getRequestURI() + "\"}"
            );
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoderConfig.passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Both JWT filters are @Component beans, which causes Spring Boot to register them as raw
    // servlet filters in addition to their security chain positions. Disabling the registration
    // here prevents each filter from executing twice per request.
    @Bean
    public FilterRegistrationBean<JwtCookieAuthFilter> disableCookieFilterServletRegistration(
            JwtCookieAuthFilter filter) {
        FilterRegistrationBean<JwtCookieAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> disableJwtAuthFilterServletRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<ApiKeyAuthenticationFilter> disableApiKeyFilterServletRegistration(
            ApiKeyAuthenticationFilter filter) {
        FilterRegistrationBean<ApiKeyAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        for (String origin : allowedOrigins) {
            if (origin.contains("*")) {
                throw new IllegalStateException(
                        "Wildcard CORS origin rejected: '" + origin + "'. " +
                        "Set app.cors.allowed-origins to explicit origins only.");
            }
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
