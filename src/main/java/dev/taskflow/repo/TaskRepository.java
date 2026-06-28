package dev.taskflow.repo;

import dev.taskflow.domain.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {
    List<TaskEntity> findByBoardId(UUID boardId);
    List<TaskEntity> findByAssigneeId(UUID assigneeId);
    List<TaskEntity> findByCreatedById(UUID createdById);
    @Query("select distinct t from TaskEntity t left join t.assigneeIds a where t.assigneeId = :userId or a = :userId")
    List<TaskEntity> findAssignedTo(@Param("userId") UUID userId);
    List<TaskEntity> findByColumnIdOrderByPositionAsc(UUID columnId);
    void deleteByColumnId(UUID columnId);
}
