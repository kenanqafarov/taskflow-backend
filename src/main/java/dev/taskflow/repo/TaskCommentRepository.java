package dev.taskflow.repo;

import dev.taskflow.domain.TaskCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskCommentRepository extends JpaRepository<TaskCommentEntity, UUID> {
    List<TaskCommentEntity> findByTaskIdOrderByCreatedAtAsc(UUID taskId);
    long countByTaskId(UUID taskId);
}
