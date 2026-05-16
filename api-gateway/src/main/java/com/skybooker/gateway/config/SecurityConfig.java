package com.skybooker.gateway.config;

import com.skybooker.gateway.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/auth/register",
                                "/auth/login",
                                "/auth/refresh",
                                "/auth/forgot-password/**",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/bookings/pnr/**",
                                "/flights/search/**",
                                "/flights/number/**",
                                "/flights/*",
                                "/seats/flight/*/map",
                                "/seats/flight/*/available",
                                "/seats/flight/*/available/*",
                                "/seats/flight/*/count/*",
                                "/airlines",
                                "/airlines/active",
                                "/airlines/**",
                                "/airports/iata/**",
                                "/airports/search",
                                "/airports/city/**",
                                "/airports/country/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/auth/v3/api-docs",
                                "/flights/v3/api-docs",
                                "/seats/v3/api-docs",
                                "/bookings/v3/api-docs",
                                "/passengers/v3/api-docs",
                                "/payments/v3/api-docs",
                                "/notifications/v3/api-docs",
                                "/airline/v3/api-docs",
                                "/actuator/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
