package com.suprith.ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.http.HttpMethod;

import com.suprith.ecommerce.security.CustomUserDetailsService;
import com.suprith.ecommerce.security.JwtAuthenticationFilter;
import com.suprith.ecommerce.security.RestAwareAccessDeniedHandler;
import com.suprith.ecommerce.security.RestAwareAuthenticationEntryPoint;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final RestAwareAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAwareAccessDeniedHandler accessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // JWT auth cookie carries SameSite=Lax which already blocks the classic
                // cross-site CSRF vector for our stateless /api/** endpoints, so CSRF
                // tokens are only required for future browser-form POSTs (e.g. admin
                // Thymeleaf forms in later modules). A cookie-based repository is used
                // instead of the session-based default so it works with STATELESS sessions.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/api/**", "/h2-console/**")
                )

                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin()) // H2 console needs same-origin framing; full clickjacking protection stays on for the rest of the app
                )

                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/", "/webjars/**", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/cart/**", "/api/wishlist/**").authenticated()
                        .requestMatchers("/api/addresses/**", "/api/orders/**", "/api/profile/**").authenticated()
                        .requestMatchers("/cart", "/wishlist", "/checkout", "/profile/**", "/orders/**").authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/api/products/**", "/api/categories/**", "/api/brands/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/products/*/reviews").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/reviews/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/reviews/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/reviews/*/like").authenticated()
                        // Storefront/browse routes are public by default; tightened per module
                        // as cart/checkout/profile controllers are added in later modules.
                        .anyRequest().permitAll()
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
