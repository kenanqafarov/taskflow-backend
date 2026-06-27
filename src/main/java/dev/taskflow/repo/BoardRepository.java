package dev.taskflow.repo;

import dev.taskflow.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface BoardRepository extends JpaRepository<BoardEntity, UUID> {}
