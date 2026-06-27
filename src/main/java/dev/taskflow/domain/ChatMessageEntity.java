package dev.taskflow.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_messages", indexes = @Index(columnList = "groupId,createdAt"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessageEntity {
    @Id @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID groupId;

    @Column(nullable = false)
    private UUID senderId;

    @Column(length = 4000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Type type;

    private String meetRoomId;
    @Column(length = 1000)
    private String mediaUrl;
    private String mediaName;
    private UUID replyToId;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) createdAt = Instant.now();
        if (type == null) type = Type.TEXT;
    }

    public enum Type { TEXT, IMAGE, VIDEO, AUDIO, FILE, MEET_INVITE, SYSTEM }
}
