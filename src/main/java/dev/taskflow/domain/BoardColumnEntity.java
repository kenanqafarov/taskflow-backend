package dev.taskflow.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "board_columns")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BoardColumnEntity {
    @Id @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID boardId;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false)
    private int position;
}
