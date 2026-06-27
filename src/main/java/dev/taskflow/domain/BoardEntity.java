package dev.taskflow.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "boards")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BoardEntity {
    @Id @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void pre() { if (createdAt == null) createdAt = Instant.now(); }
}
