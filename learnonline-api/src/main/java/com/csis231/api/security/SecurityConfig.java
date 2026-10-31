package com.csis231.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.security.cors.allowed-origin-patterns:http://localhost:3000,http://localhost:5173,http://127.0.0.1:3000,http://127.0.0.1:5173}")
            List<String> allowedOriginPatterns) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOriginPatterns);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Coarse, URL-level access rules; the first matching rule wins. Ownership and enrollment
     * rules need database state, so they live in the services.
     *
     * <p>CSRF protection is off because credentials travel in the {@code Authorization} header,
     * which a browser never attaches on its own, and there is no session cookie to abuse.</p>
     *
     * <p>The JWT filter is created here rather than declared as a bean: Spring Boot registers every
     * {@code Filter} bean with the servlet container, which would add it a second time outside this chain.</p>
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           CorsConfigurationSource corsConfigurationSource,
                                           JwtService jwtService,
                                           UserDetailsService userDetailsService) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/csis-users/me").authenticated()
                        .requestMatchers("/api/csis-users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/courses/*/enrollments").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/courses/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/courses/**").hasAnyRole("INSTRUCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/courses/**").hasAnyRole("INSTRUCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/courses/**").hasAnyRole("INSTRUCTOR", "ADMIN")
                        .requestMatchers("/api/instructor/**", "/api/instructors/**").hasAnyRole("INSTRUCTOR", "ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
