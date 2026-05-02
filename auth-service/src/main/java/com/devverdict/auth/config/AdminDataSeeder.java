package com.devverdict.auth.config;

import com.devverdict.auth.domain.Role;
import com.devverdict.auth.domain.User;
import com.devverdict.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

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
        seedAdmin();
        seedDummyUsers();
    }

    private void seedAdmin() {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            logger.info("Default admin user already exists, skipping admin seed");
            return;
        }

        String passwordHash = passwordEncoder.encode(ADMIN_PASSWORD);
        User admin = new User(ADMIN_EMAIL, ADMIN_USERNAME, passwordHash, Role.ADMIN);
        userRepository.save(admin);

        logger.info("Created default admin user: {} / {}", ADMIN_EMAIL, ADMIN_USERNAME);
    }

    private void seedDummyUsers() {
        if (userRepository.count() > 1) {
            logger.info("Dummy users already exist, skipping user seed");
            return;
        }

        String passwordHash = passwordEncoder.encode("password123");

        List<User> dummyUsers = List.of(
            new User("alice@example.com", "alice", passwordHash, Role.USER),
            new User("bob@example.com", "bob", passwordHash, Role.USER),
            new User("charlie@example.com", "charlie", passwordHash, Role.USER),
            new User("diana@example.com", "diana", passwordHash, Role.USER),
            new User("evan@example.com", "evan", passwordHash, Role.USER),
            new User("fiona@example.com", "fiona", passwordHash, Role.USER),
            new User("george@example.com", "george", passwordHash, Role.USER),
            new User("hannah@example.com", "hannah", passwordHash, Role.USER),
            new User("ian@example.com", "ian", passwordHash, Role.USER),
            new User("julia@example.com", "julia", passwordHash, Role.USER),
            new User("kevin@example.com", "kevin", passwordHash, Role.USER),
            new User("laura@example.com", "laura", passwordHash, Role.USER),
            new User("mike@example.com", "mike", passwordHash, Role.USER),
            new User("nina@example.com", "nina", passwordHash, Role.USER),
            new User("oscar@example.com", "oscar", passwordHash, Role.USER)
        );

        userRepository.saveAll(dummyUsers);
        logger.info("Created {} dummy users for demo data", dummyUsers.size());
    }
}
