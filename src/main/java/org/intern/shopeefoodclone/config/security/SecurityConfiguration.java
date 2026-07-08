package org.intern.shopeefoodclone.config.security;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
class SecurityConfiguration {

    private JwtService jwtService;

    private static final String[] WHITELIST = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/verify-otp",
            "/api/auth/send-otp",

            "/docs",
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize.requestMatchers(WHITELIST).permitAll()
                        .anyRequest().authenticated())
                .addFilterAfter(new JwtAuthFilter(jwtService), BasicAuthenticationFilter.class)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new JwtAuthEntryPoint()))
        ;
        return http.build();
    }

}
