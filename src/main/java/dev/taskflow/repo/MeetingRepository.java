package dev.taskflow.repo;

import dev.taskflow.domain.MeetingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

public interface MeetingRepository extends JpaRepository<MeetingEntity, UUID> {
    List<MeetingEntity> findAllByOrderByScheduledAtAsc();
    Optional<MeetingEntity> findByRoomId(String roomId);
    void deleteByScheduledAtBefore(Instant cutoff);
}
