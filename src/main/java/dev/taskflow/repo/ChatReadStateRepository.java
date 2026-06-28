package dev.taskflow.repo;

import dev.taskflow.domain.ChatReadStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChatReadStateRepository extends JpaRepository<ChatReadStateEntity, UUID> {
    Optional<ChatReadStateEntity> findByGroupIdAndUserId(UUID groupId, UUID userId);
}
