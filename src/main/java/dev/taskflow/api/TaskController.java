package dev.taskflow.api;

import dev.taskflow.api.Dtos.*;
import dev.taskflow.domain.TaskEntity;
import dev.taskflow.repo.TaskRepository;
import dev.taskflow.repo.TaskCommentRepository;
import dev.taskflow.repo.UserRepository;
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
    private final TaskCommentRepository comments;
    private final UserRepository users;
    private final dev.taskflow.repo.NotificationRepository notifications;

    public TaskController(TaskRepository tasks, TaskCommentRepository comments, UserRepository users,
                          dev.taskflow.repo.NotificationRepository notifications) {
        this.tasks = tasks; this.comments = comments; this.users = users; this.notifications = notifications;
    }

    @GetMapping("/me")
    public List<TaskDto> mine() {
        var me = CurrentUser.require();
        return tasks.findAssignedTo(me.id()).stream().map(TaskDto::of).toList();
    }

    @GetMapping("/created")
    public List<TaskDto> created() {
        var me = CurrentUser.require();
        return tasks.findByCreatedById(me.id()).stream().map(TaskDto::of).toList();
    }

    @GetMapping("/overview")
    public List<TaskDto> overview() {
        var me = CurrentUser.require();
        var items = "SUPER_ADMIN".equals(me.role()) ? tasks.findAll() : tasks.findAssignedTo(me.id());
        return items.stream().map(TaskDto::of).toList();
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
        if (req.assigneeIds() != null) t.setAssigneeIds(new java.util.HashSet<>(req.assigneeIds()));
        t.setDueDate(req.dueDate());
        var saved = tasks.save(t);
        if (req.assigneeIds() != null) {
            var me = CurrentUser.require();
            req.assigneeIds().stream().filter(userId -> !userId.equals(me.id()))
                    .filter(userId -> users.findById(userId).map(user -> !Boolean.FALSE.equals(user.getNotificationsEnabled())).orElse(false))
                    .forEach(userId ->
                    notifications.save(dev.taskflow.domain.NotificationEntity.builder()
                            .userId(userId).text("You were assigned to " + saved.getTitle())
                            .type("ASSIGNED").resourceId(saved.getId())
                            .actionUrl("/boards/" + saved.getBoardId() + "?task=" + saved.getId()).build()));
        }
        return TaskDto.of(saved);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        CurrentUser.require();
        if (!tasks.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        tasks.deleteById(id);
    }

    @GetMapping("/{id}/comments")
    public List<TaskCommentDto> comments(@PathVariable UUID id) {
        CurrentUser.require();
        if (!tasks.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return comments.findByTaskIdOrderByCreatedAtAsc(id).stream()
                .map(comment -> TaskCommentDto.of(comment, users.findById(comment.getAuthorId()).orElse(null)))
                .toList();
    }

    @PostMapping("/{id}/comments")
    public TaskCommentDto comment(@PathVariable UUID id, @RequestBody CreateCommentReq req) {
        var me = CurrentUser.require();
        if (!tasks.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (req.content() == null || req.content().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment is required");
        }
        var saved = comments.save(dev.taskflow.domain.TaskCommentEntity.builder()
                .taskId(id).authorId(me.id()).content(req.content().trim()).build());
        return TaskCommentDto.of(saved, users.findById(me.id()).orElse(null));
    }

    @PostMapping("/{id}/assign")
    public TaskDto assign(@PathVariable UUID id, @RequestBody AssignTaskReq req) {
        CurrentUser.require();
        TaskEntity t = tasks.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        t.setAssigneeId(req.assigneeId());
        t.setAssigneeIds(req.assigneeId() == null
                ? new java.util.HashSet<>()
                : new java.util.HashSet<>(java.util.Set.of(req.assigneeId())));
        return TaskDto.of(tasks.save(t));
    }
}
