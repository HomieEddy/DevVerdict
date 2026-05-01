package com.devverdict.auth.config;

import com.devverdict.auth.domain.Role;
import com.devverdict.auth.domain.User;
import com.devverdict.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminDataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminDataSeeder.class);
    private static final String ADMIN_EMAIL = "admin@devverdict.com";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminDataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            logger.info("Default admin user already exists, skipping seed");
            return;
        }

        String passwordHash = passwordEncoder.encode(ADMIN_PASSWORD);
        User admin = new User(ADMIN_EMAIL, ADMIN_USERNAME, passwordHash, Role.ADMIN);
        userRepository.save(admin);

        logger.info("Created default admin user: {} / {}", ADMIN_EMAIL, ADMIN_USERNAME);
    }
}
