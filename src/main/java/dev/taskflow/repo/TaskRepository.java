package dev.taskflow.repo;

import dev.taskflow.domain.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {
    List<TaskEntity> findByBoardId(UUID boardId);
    List<TaskEntity> findByAssigneeId(UUID assigneeId);
    List<TaskEntity> findByColumnIdOrderByPositionAsc(UUID columnId);
    void deleteByColumnId(UUID columnId);
}
