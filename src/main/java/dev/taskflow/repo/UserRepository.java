package dev.taskflow.repo;

import dev.taskflow.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findFirstByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);
    boolean existsByUsername(String username);
}
