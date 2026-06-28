package dev.taskflow.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "meetings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MeetingEntity {
    @Id @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String roomId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private UUID organizerId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "meeting_participants", joinColumns = @JoinColumn(name = "meeting_id"))
    @Column(name = "user_id")
    @Builder.Default
    private Set<UUID> participantIds = new HashSet<>();

    private Instant scheduledAt;
    private Integer durationMinutes;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (durationMinutes == null) durationMinutes = 30;
    }
}
