package dev.taskflow.bootstrap;

import dev.taskflow.domain.UserEntity;
import dev.taskflow.repo.ChatGroupRepository;
import dev.taskflow.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class Seeder implements CommandLineRunner {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final ChatGroupRepository groups;
    private final String adminUser;
    private final String adminPass;

    public Seeder(UserRepository users,
                  PasswordEncoder encoder,
                  ChatGroupRepository groups,
                  @Value("${taskflow.seed.super-admin.username}") String adminUser,
                  @Value("${taskflow.seed.super-admin.password}") String adminPass) {
        this.users = users; this.encoder = encoder; this.groups = groups;
        this.adminUser = adminUser; this.adminPass = adminPass;
    }

    @Override
    public void run(String... args) {
        if (!users.existsByUsername(adminUser)) {
            users.save(UserEntity.builder()
                    .username(adminUser)
                    .passwordHash(encoder.encode(adminPass))
                    .firstName("Super")
                    .lastName("Admin")
                    .role(UserEntity.Role.SUPER_ADMIN)
                    .build());
        }
        UserEntity bot = users.findByUsername("tasko_ai").orElseGet(() ->
                users.save(UserEntity.builder()
                        .username("tasko_ai")
                        .passwordHash(encoder.encode(UUID.randomUUID().toString()))
                        .firstName("Tasko")
                        .lastName("AI")
                        .email("tasko-ai@local")
                        .role(UserEntity.Role.MEMBER)
                        .build()));
        groups.findAll().forEach(group -> {
            if (group.getMemberIds().add(bot.getId())) groups.save(group);
        });
    }
}
