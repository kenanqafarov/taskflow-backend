package dev.taskflow.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "chat_groups")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatGroupEntity {
    @Id @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(length = 1000)
    private String avatarUrl;

    @Column(nullable = false)
    private boolean direct;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "chat_group_members", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "user_id")
    @Builder.Default
    private Set<UUID> memberIds = new HashSet<>();

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void pre() { if (createdAt == null) createdAt = Instant.now(); }
}
