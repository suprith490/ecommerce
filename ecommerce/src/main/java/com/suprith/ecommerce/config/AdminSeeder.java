package com.suprith.ecommerce.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.suprith.ecommerce.entity.User;
import com.suprith.ecommerce.enums.Role;
import com.suprith.ecommerce.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The H2 database is in-memory, so it resets on every restart, and the normal
 * registration flow always assigns Role.CUSTOMER (see AuthServiceImpl). Without
 * this seeder there is no way to ever reach an /api/admin/** endpoint, since
 * SecurityConfig requires ROLE_ADMIN for all of them.
 *
 * Runs once at startup and only creates the account if it doesn't already exist,
 * so it's safe to leave in place even if you later add real admin management.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@suprithstore.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@123}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        User admin = User.builder()
                .name("Store Admin")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        userRepository.save(admin);

        log.info("Seeded default admin account -> email: {}, password: {}", adminEmail, adminPassword);
    }
}
