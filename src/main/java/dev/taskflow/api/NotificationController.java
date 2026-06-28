package dev.taskflow.api;

import dev.taskflow.api.Dtos.CreateNotificationReq;
import dev.taskflow.api.Dtos.NotificationDto;
import dev.taskflow.domain.NotificationEntity;
import dev.taskflow.repo.NotificationRepository;
import dev.taskflow.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationRepository notifications;

    public NotificationController(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public List<NotificationDto> list() {
        var me = CurrentUser.require();
        return notifications.findByUserIdOrderByCreatedAtDesc(me.id()).stream()
                .map(NotificationDto::of)
                .toList();
    }

    @PostMapping
    public NotificationDto create(@RequestBody CreateNotificationReq req) {
        CurrentUser.requireAdmin();
        if (req.userId() == null || req.text() == null || req.text().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId and text are required");
        }
        return NotificationDto.of(notifications.save(NotificationEntity.builder()
                .userId(req.userId())
                .text(req.text().trim())
                .type(req.type())
                .build()));
    }

    @PatchMapping("/{id}/read")
    public NotificationDto markRead(@PathVariable UUID id) {
        var me = CurrentUser.require();
        var notification = notifications.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!notification.getUserId().equals(me.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        notification.setRead(true);
        return NotificationDto.of(notifications.save(notification));
    }

    @PatchMapping("/read-all")
    public List<NotificationDto> markAllRead() {
        var me = CurrentUser.require();
        var items = notifications.findByUserIdOrderByCreatedAtDesc(me.id());
        items.forEach(item -> item.setRead(true));
        return notifications.saveAll(items).stream().map(NotificationDto::of).toList();
    }
}
