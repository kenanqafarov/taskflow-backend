package dev.taskflow.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_comments", indexes = @Index(columnList = "taskId,createdAt"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaskCommentEntity {
    @Id @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID taskId;

    @Column(nullable = false)
    private UUID authorId;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
