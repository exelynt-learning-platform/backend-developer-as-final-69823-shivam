package com.exelynt.booking.config;

import com.exelynt.booking.entity.Resource;
import com.exelynt.booking.entity.User;
import com.exelynt.booking.enums.Role;
import com.exelynt.booking.repository.ResourceRepository;
import com.exelynt.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds the accounts and resources needed to exercise the API.
 *
 * <p>Implemented as a {@link CommandLineRunner} rather than {@code data.sql} for two reasons:
 * passwords are hashed at runtime by the configured {@link PasswordEncoder} instead of being
 * committed as pre-computed hashes, and it sidesteps the ordering problem where {@code data.sql}
 * runs before Hibernate has finished creating the schema under {@code ddl-auto=update}.
 *
 * <p>Idempotent: it inspects each table and only inserts when empty, so restarts do not
 * duplicate rows.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        seedResources();
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            log.info("Users already present - skipping user seed");
            return;
        }

        List<User> users = List.of(
                buildUser("admin", "Admin@123", Role.ADMIN),
                buildUser("alice", "User@123", Role.USER),
                buildUser("bob", "User@123", Role.USER)
        );
        userRepository.saveAll(users);

        log.info("Seeded {} users: {}", users.size(),
                users.stream().map(User::getUsername).toList());
    }

    private User buildUser(String username, String rawPassword, Role role) {
        return User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .build();
    }

    private void seedResources() {
        if (resourceRepository.count() > 0) {
            log.info("Resources already present - skipping resource seed");
            return;
        }

        List<Resource> resources = List.of(
                buildResource("Conference Room A", "ROOM",
                        "Seats 12, projector and whiteboard", true, "150.00"),
                buildResource("Conference Room B", "ROOM",
                        "Seats 4, video conferencing", true, "80.00"),
                buildResource("Company Car - Sedan", "VEHICLE",
                        "5 seats, diesel", true, "220.50"),
                buildResource("4K Projector", "EQUIPMENT",
                        "Portable, HDMI and USB-C", true, "45.00"),
                buildResource("Drone - Survey Unit", "EQUIPMENT",
                        "Grounded for maintenance", false, "300.00")
        );
        resourceRepository.saveAll(resources);

        log.info("Seeded {} resources ({} unavailable, for testing rejection paths)",
                resources.size(), resources.stream().filter(r -> !r.isAvailable()).count());
    }

    private Resource buildResource(String name, String type, String description,
                                   boolean available, String pricePerUnit) {
        return Resource.builder()
                .name(name)
                .type(type)
                .description(description)
                .available(available)
                .pricePerUnit(new BigDecimal(pricePerUnit))
                .build();
    }
}
