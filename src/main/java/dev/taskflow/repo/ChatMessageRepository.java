package dev.taskflow.repo;

import dev.taskflow.domain.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID> {
    List<ChatMessageEntity> findByGroupIdOrderByCreatedAtAsc(UUID groupId);
}
