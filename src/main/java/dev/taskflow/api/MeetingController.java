package dev.taskflow.api;

import dev.taskflow.api.Dtos.CreateMeetingReq;
import dev.taskflow.api.Dtos.MeetingDto;
import dev.taskflow.domain.MeetingEntity;
import dev.taskflow.repo.MeetingRepository;
import dev.taskflow.repo.NotificationRepository;
import dev.taskflow.repo.UserRepository;
import dev.taskflow.domain.NotificationEntity;
import dev.taskflow.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {
    private final MeetingRepository meetings;
    private final NotificationRepository notifications;
    private final UserRepository users;

    public MeetingController(MeetingRepository meetings, NotificationRepository notifications,
                             UserRepository users) {
        this.meetings = meetings; this.notifications = notifications; this.users = users;
    }

    @GetMapping
    public List<MeetingDto> list() {
        var me = CurrentUser.require();
        return meetings.findAllByOrderByScheduledAtAsc().stream()
                .filter(meeting -> meeting.getOrganizerId().equals(me.id())
                        || meeting.getParticipantIds().contains(me.id()))
                .map(MeetingDto::of)
                .toList();
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void removeExpiredMeetings() {
        meetings.deleteByScheduledAtBefore(java.time.Instant.now().minus(java.time.Duration.ofHours(2)));
    }

    @PostMapping
    public MeetingDto create(@RequestBody CreateMeetingReq req) {
        var me = CurrentUser.require();
        if (!me.isAdmin()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only");
        if (req.title() == null || req.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }
        var participants = req.participantIds() == null
                ? new HashSet<UUID>()
                : new HashSet<>(req.participantIds());
        participants.add(me.id());
        var meeting = MeetingEntity.builder()
                .roomId("tf-" + UUID.randomUUID().toString().substring(0, 12))
                .title(req.title().trim())
                .description(req.description())
                .organizerId(me.id())
                .participantIds(participants)
                .scheduledAt(req.scheduledAt())
                .durationMinutes(req.durationMinutes())
                .build();
        var saved = meetings.save(meeting);
        participants.stream().filter(userId -> !userId.equals(me.id()))
                .filter(userId -> users.findById(userId).map(user -> !Boolean.FALSE.equals(user.getNotificationsEnabled())).orElse(false))
                .forEach(userId ->
                notifications.save(NotificationEntity.builder()
                        .userId(userId).text("Meeting scheduled: " + saved.getTitle())
                        .type("MEETING").resourceId(saved.getId())
                        .actionUrl("/meetings").build()));
        return MeetingDto.of(saved);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        var me = CurrentUser.require();
        var meeting = meetings.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!meeting.getOrganizerId().equals(me.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        meetings.delete(meeting);
    }
}
