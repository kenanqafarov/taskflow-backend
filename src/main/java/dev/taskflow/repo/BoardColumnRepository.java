package dev.taskflow.repo;

import dev.taskflow.domain.BoardColumnEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BoardColumnRepository extends JpaRepository<BoardColumnEntity, UUID> {
    List<BoardColumnEntity> findByBoardIdOrderByPositionAsc(UUID boardId);
}
