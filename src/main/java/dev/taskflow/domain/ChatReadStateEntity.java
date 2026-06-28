package dev.taskflow.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_read_states", uniqueConstraints = @UniqueConstraint(columnNames = {"groupId", "userId"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatReadStateEntity {
    @Id @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID groupId;

    @Column(nullable = false)
    private UUID userId;

    private UUID lastReadMessageId;
    private UUID scrollMessageId;
    private Instant lastReadAt;
}
