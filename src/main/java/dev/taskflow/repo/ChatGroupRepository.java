package dev.taskflow.repo;

import dev.taskflow.domain.ChatGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface ChatGroupRepository extends JpaRepository<ChatGroupEntity, UUID> {
    @Query("select g from ChatGroupEntity g where :uid member of g.memberIds")
    List<ChatGroupEntity> findAllForUser(@Param("uid") UUID userId);
}
