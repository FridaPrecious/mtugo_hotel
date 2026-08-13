package com.mtugo.mtugo_hotel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Protects /staff/dashboard (the page) and /api/staff/** (the API) with HTTP
 * Basic Authentication, per staff-requirements.md AC-1 and AC-10. Everything
 * else (menu, cart, checkout, payment pages, M-Pesa callback, H2 console)
 * stays publicly accessible.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${staff.username:staff}")
    private String staffUsername;

    @Value("${staff.password:mtugo2026}")
    private String staffPassword;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(PasswordEncoder encoder) {
        UserDetails staff = User.builder()
                .username(staffUsername)
                .password(encoder.encode(staffPassword))
                .roles("STAFF")
                .build();
        return new InMemoryUserDetailsManager(staff);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Stateless JSON APIs behind Basic Auth don't need CSRF tokens, and the
            // H2 console + M-Pesa callback aren't form-based either.
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/staff/**", "/api/staff/**").hasRole("STAFF")
                    .anyRequest().permitAll()
            )
            .httpBasic(basic -> {})
            // H2 console renders inside a frame; Spring Security blocks frames by default.
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}
