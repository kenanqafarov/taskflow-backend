package dev.taskflow.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = @Index(columnList = "username", unique = true))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserEntity {
    @Id @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 80)
    private String firstName;

    @Column(nullable = false, length = 80)
    private String lastName;

    private String email;
    private String githubUrl;
    private String linkedinUrl;
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (role == null) role = Role.MEMBER;
    }

    public enum Role { SUPER_ADMIN, ADMIN, MEMBER }
}
