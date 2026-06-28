package dev.taskflow.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = @Index(columnList = "userId,createdAt"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationEntity {
    @Id @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 500)
    private String text;

    @Column(nullable = false, length = 32)
    private String type;

    private UUID resourceId;

    @Column(length = 500)
    private String actionUrl;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (type == null || type.isBlank()) type = "INFO";
    }
}
