package dev.taskflow.repo;

import dev.taskflow.domain.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;
import java.time.Instant;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID> {
    List<ChatMessageEntity> findByGroupIdOrderByCreatedAtAsc(UUID groupId);
    long countByGroupIdAndCreatedAtAfterAndSenderIdNot(UUID groupId, Instant createdAt, UUID senderId);
    @Query("select count(distinct m) from ChatMessageEntity m join m.mentionIds u where m.groupId = :groupId and m.createdAt > :after and u = :userId")
    long countMentionsAfter(@Param("groupId") UUID groupId, @Param("userId") UUID userId, @Param("after") Instant after);
}
