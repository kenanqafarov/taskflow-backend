package dev.taskflow.api;

import dev.taskflow.api.Dtos.*;
import dev.taskflow.domain.*;
import dev.taskflow.repo.*;
import dev.taskflow.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class BoardController {
    private final BoardRepository boards;
    private final BoardColumnRepository columns;
    private final TaskRepository tasks;

    public BoardController(BoardRepository boards, BoardColumnRepository columns, TaskRepository tasks) {
        this.boards = boards; this.columns = columns; this.tasks = tasks;
    }

    @GetMapping("/boards")
    public List<BoardDto> list() {
        CurrentUser.require();
        return boards.findAll().stream().map(BoardDto::of).toList();
    }

    @PostMapping("/boards")
    @Transactional
    public BoardDto create(@RequestBody CreateBoardReq req) {
        var me = CurrentUser.require();
        if (req.name() == null || req.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name required");
        }
        var b = boards.save(BoardEntity.builder()
                .name(req.name())
                .description(req.description())
                .ownerId(me.id())
                .build());
        // seed default columns
        String[] defaults = {"Backlog", "In progress", "Review", "Done"};
        for (int i = 0; i < defaults.length; i++) {
            columns.save(BoardColumnEntity.builder().boardId(b.getId()).name(defaults[i]).position(i).build());
        }
        return BoardDto.of(b);
    }

    @GetMapping("/boards/{id}")
    public BoardDetail get(@PathVariable UUID id) {
        CurrentUser.require();
        var b = boards.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var cols = columns.findByBoardIdOrderByPositionAsc(id).stream().map(ColumnDto::of).toList();
        var ts = tasks.findByBoardId(id).stream().map(TaskDto::of).toList();
        return new BoardDetail(BoardDto.of(b), cols, ts);
    }

    @PostMapping("/boards/{id}/columns")
    public ColumnDto addColumn(@PathVariable UUID id, @RequestBody CreateColumnReq req) {
        CurrentUser.require();
        int pos = columns.findByBoardIdOrderByPositionAsc(id).size();
        var c = columns.save(BoardColumnEntity.builder().boardId(id).name(req.name()).position(pos).build());
        return ColumnDto.of(c);
    }

    @PutMapping("/columns/{id}")
    public ColumnDto updateColumn(@PathVariable UUID id, @RequestBody UpdateColumnReq req) {
        CurrentUser.require();
        var column = columns.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));
        if (req.name() == null || req.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "List name is required");
        }
        column.setName(req.name().trim());
        return ColumnDto.of(columns.save(column));
    }

    @PostMapping("/columns/{id}/move")
    @Transactional
    public ColumnDto moveColumn(@PathVariable UUID id, @RequestBody MoveColumnReq req) {
        CurrentUser.require();
        var column = columns.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));
        var ordered = new java.util.ArrayList<>(columns.findByBoardIdOrderByPositionAsc(column.getBoardId()));
        ordered.removeIf(item -> item.getId().equals(id));
        int position = req.position() == null ? column.getPosition() : req.position();
        position = Math.max(0, Math.min(position, ordered.size()));
        ordered.add(position, column);
        for (int i = 0; i < ordered.size(); i++) ordered.get(i).setPosition(i);
        columns.saveAll(ordered);
        return ColumnDto.of(column);
    }

    @DeleteMapping("/columns/{id}")
    @Transactional
    public void deleteColumn(@PathVariable UUID id) {
        CurrentUser.require();
        var column = columns.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));
        tasks.deleteByColumnId(id);
        columns.delete(column);
        var ordered = columns.findByBoardIdOrderByPositionAsc(column.getBoardId());
        for (int i = 0; i < ordered.size(); i++) ordered.get(i).setPosition(i);
        columns.saveAll(ordered);
    }

    @PostMapping("/boards/{id}/tasks")
    public TaskDto addTask(@PathVariable UUID id, @RequestBody CreateTaskReq req) {
        CurrentUser.require();
        if (req.columnId() == null || req.title() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "columnId and title are required");
        }
        int pos = tasks.findByColumnIdOrderByPositionAsc(req.columnId()).size();
        TaskEntity.Priority pr = TaskEntity.Priority.MEDIUM;
        if (req.priority() != null) { try { pr = TaskEntity.Priority.valueOf(req.priority()); } catch (Exception ignored) {} }
        var t = tasks.save(TaskEntity.builder()
                .boardId(id)
                .columnId(req.columnId())
                .title(req.title())
                .description(req.description())
                .position(pos)
                .priority(pr)
                .assigneeId(req.assigneeId())
                .build());
        return TaskDto.of(t);
    }
}
