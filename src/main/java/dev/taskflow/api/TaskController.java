package dev.taskflow.api;

import dev.taskflow.api.Dtos.*;
import dev.taskflow.domain.TaskEntity;
import dev.taskflow.repo.TaskRepository;
import dev.taskflow.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskRepository tasks;

    public TaskController(TaskRepository tasks) { this.tasks = tasks; }

    @GetMapping("/me")
    public List<TaskDto> mine() {
        var me = CurrentUser.require();
        return tasks.findByAssigneeId(me.id()).stream().map(TaskDto::of).toList();
    }

    @PostMapping("/{id}/move")
    @Transactional
    public TaskDto move(@PathVariable UUID id, @RequestBody MoveTaskReq req) {
        CurrentUser.require();
        TaskEntity t = tasks.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        UUID targetColumn = req.columnId() == null ? t.getColumnId() : req.columnId();
        int targetPosition = req.position() == null ? t.getPosition() : Math.max(0, req.position());
        var source = tasks.findByColumnIdOrderByPositionAsc(t.getColumnId());
        source.removeIf(item -> item.getId().equals(id));
        for (int i = 0; i < source.size(); i++) {
            source.get(i).setPosition(i);
        }
        tasks.saveAll(source);
        var target = t.getColumnId().equals(targetColumn) ? source : tasks.findByColumnIdOrderByPositionAsc(targetColumn);
        target.removeIf(item -> item.getId().equals(id));
        targetPosition = Math.min(targetPosition, target.size());
        t.setColumnId(targetColumn);
        target.add(targetPosition, t);
        for (int i = 0; i < target.size(); i++) target.get(i).setPosition(i);
        tasks.saveAll(target);
        return TaskDto.of(tasks.save(t));
    }

    @PutMapping("/{id}")
    public TaskDto update(@PathVariable UUID id, @RequestBody UpdateTaskReq req) {
        CurrentUser.require();
        TaskEntity t = tasks.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (req.title() != null && !req.title().isBlank()) t.setTitle(req.title().trim());
        if (req.description() != null) t.setDescription(req.description());
        if (req.priority() != null) {
            try { t.setPriority(TaskEntity.Priority.valueOf(req.priority())); } catch (IllegalArgumentException ignored) {}
        }
        t.setAssigneeId(req.assigneeId());
        t.setDueDate(req.dueDate());
        return TaskDto.of(tasks.save(t));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        CurrentUser.require();
        if (!tasks.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        tasks.deleteById(id);
    }

    @PostMapping("/{id}/assign")
    public TaskDto assign(@PathVariable UUID id, @RequestBody AssignTaskReq req) {
        CurrentUser.require();
        TaskEntity t = tasks.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        t.setAssigneeId(req.assigneeId());
        return TaskDto.of(tasks.save(t));
    }
}
