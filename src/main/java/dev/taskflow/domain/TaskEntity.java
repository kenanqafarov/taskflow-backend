package dev.taskflow.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tasks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaskEntity {
    @Id @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID boardId;

    @Column(nullable = false)
    private UUID columnId;

    @Column(nullable = false, length = 240)
    private String title;

    @Column(length = 4000)
    private String description;

    @Column(nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Priority priority;

    private UUID assigneeId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_assignees", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "user_id")
    @Builder.Default
    private Set<UUID> assigneeIds = new HashSet<>();

    private UUID createdById;
    private Instant dueDate;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) createdAt = Instant.now();
        if (priority == null) priority = Priority.MEDIUM;
    }

    public enum Priority { LOW, MEDIUM, HIGH, URGENT }
}
