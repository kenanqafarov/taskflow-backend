package dev.taskflow.api;

import dev.taskflow.domain.UserEntity;
import dev.taskflow.domain.TaskEntity;
import dev.taskflow.domain.BoardEntity;
import dev.taskflow.domain.BoardColumnEntity;
import dev.taskflow.domain.ChatGroupEntity;
import dev.taskflow.domain.ChatMessageEntity;
import dev.taskflow.domain.MeetingEntity;
import dev.taskflow.domain.NotificationEntity;
import dev.taskflow.domain.TaskCommentEntity;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class Dtos {
    private Dtos() {}

    public record UserDto(UUID id, String username, String firstName, String lastName,
                          String email, String githubUrl, String linkedinUrl, String avatarUrl,
                          String role, boolean active, boolean notificationsEnabled, String theme,
                          Instant createdAt) {
        public static UserDto of(UserEntity u) {
            return new UserDto(u.getId(), u.getUsername(), u.getFirstName(), u.getLastName(),
                    u.getEmail(), u.getGithubUrl(), u.getLinkedinUrl(), u.getAvatarUrl(),
                    u.getRole().name(), u.isActive(), !Boolean.FALSE.equals(u.getNotificationsEnabled()),
                    u.getTheme(), u.getCreatedAt());
        }
    }

    public record LoginReq(String username, String password) {}
    public record LoginRes(String token, UserDto user) {}

    public record CreateUserReq(String username, String password, String firstName, String lastName,
                                String email, String githubUrl, String linkedinUrl, String avatarUrl, String role) {}
    public record UpdateUserReq(String username, String password, String firstName, String lastName,
                                String email, String githubUrl, String linkedinUrl, String avatarUrl,
                                String role, Boolean active) {}
    public record UpdateProfileReq(String username, String firstName, String lastName,
                                   String email, String githubUrl, String linkedinUrl, String avatarUrl,
                                   String currentPassword, String newPassword,
                                   Boolean notificationsEnabled, String theme) {}

    public record BoardDto(UUID id, String name, String description, UUID ownerId,
                           boolean archived, boolean favorite, Instant createdAt) {
        public static BoardDto of(BoardEntity b, UUID viewerId) {
            return new BoardDto(b.getId(), b.getName(), b.getDescription(), b.getOwnerId(),
                    b.isArchived(), b.getFavoriteByIds().contains(viewerId), b.getCreatedAt());
        }
    }
    public record CreateBoardReq(String name, String description) {}
    public record UpdateBoardReq(String name, String description, Boolean archived, Boolean favorite) {}

    public record ColumnDto(UUID id, UUID boardId, String name, int position) {
        public static ColumnDto of(BoardColumnEntity c) {
            return new ColumnDto(c.getId(), c.getBoardId(), c.getName(), c.getPosition());
        }
    }
    public record CreateColumnReq(String name) {}
    public record UpdateColumnReq(String name) {}
    public record MoveColumnReq(Integer position) {}

    public record TaskDto(UUID id, UUID boardId, UUID columnId, String title, String description,
                          int position, String priority, UUID assigneeId, Set<UUID> assigneeIds,
                          UUID createdById, Instant dueDate, long commentCount, Instant createdAt) {
        public static TaskDto of(TaskEntity t) {
            return new TaskDto(t.getId(), t.getBoardId(), t.getColumnId(), t.getTitle(), t.getDescription(),
                    t.getPosition(), t.getPriority().name(), t.getAssigneeId(), t.getAssigneeIds(),
                    t.getCreatedById(), t.getDueDate(), 0, t.getCreatedAt());
        }
    }
    public record CreateTaskReq(UUID columnId, String title, String description, String priority,
                                UUID assigneeId, Set<UUID> assigneeIds, Instant dueDate) {}
    public record UpdateTaskReq(String title, String description, String priority, UUID assigneeId,
                                Set<UUID> assigneeIds, Instant dueDate) {}
    public record MoveTaskReq(UUID columnId, Integer position) {}
    public record AssignTaskReq(UUID assigneeId) {}

    public record BoardDetail(BoardDto board, List<ColumnDto> columns, List<TaskDto> tasks) {}

    public record ChatGroupDto(UUID id, String name, String description, String avatarUrl,
                               boolean isDirect, Set<UUID> memberIds, long unreadCount, long mentionCount,
                               UUID lastReadMessageId, UUID scrollMessageId, Instant createdAt) {
        public static ChatGroupDto of(ChatGroupEntity g) {
            return new ChatGroupDto(g.getId(), g.getName(), g.getDescription(), g.getAvatarUrl(),
                    g.isDirect(), g.getMemberIds(), 0, 0, null, null, g.getCreatedAt());
        }
    }
    public record CreateGroupReq(String name, String description, String avatarUrl, List<UUID> memberIds) {}
    public record UpdateGroupReq(String name, String description, String avatarUrl, List<UUID> memberIds) {}

    public record ChatMessageDto(UUID id, UUID groupId, UUID senderId, UserDto sender,
                                 String content, String type, String meetRoomId, String mediaUrl,
                                 String mediaName, UUID replyToId, ChatMessageDto replyTo,
                                 Set<UUID> mentionIds, Instant createdAt) {}
    public record SendMessageReq(String content, String type, String meetRoomId,
                                 String mediaUrl, String mediaName, UUID replyToId) {}
    public record SocketEvent(String event, Object data) {}
    public record MarkChatReadReq(UUID lastReadMessageId, UUID scrollMessageId) {}

    public record StartMeetReq(UUID groupId) {}
    public record StartMeetRes(String roomId, String url) {}
    public record MeetTokenRes(String token, String url) {}

    public record NotificationDto(UUID id, String text, String type, UUID resourceId,
                                  String actionUrl, boolean read, Instant createdAt) {
        public static NotificationDto of(NotificationEntity notification) {
            return new NotificationDto(notification.getId(), notification.getText(), notification.getType(),
                    notification.getResourceId(), notification.getActionUrl(),
                    notification.isRead(), notification.getCreatedAt());
        }
    }
    public record CreateNotificationReq(UUID userId, String text, String type) {}

    public record MeetingDto(UUID id, String roomId, String title, String description, UUID organizerId,
                             Set<UUID> participantIds, Instant scheduledAt, Integer durationMinutes,
                             Instant createdAt) {
        public static MeetingDto of(MeetingEntity meeting) {
            return new MeetingDto(meeting.getId(), meeting.getRoomId(), meeting.getTitle(),
                    meeting.getDescription(), meeting.getOrganizerId(), meeting.getParticipantIds(),
                    meeting.getScheduledAt(), meeting.getDurationMinutes(), meeting.getCreatedAt());
        }
    }
    public record CreateMeetingReq(String title, String description, Set<UUID> participantIds,
                                   Instant scheduledAt, Integer durationMinutes) {}

    public record TaskCommentDto(UUID id, UUID taskId, UUID authorId, UserDto author,
                                 String content, Instant createdAt) {
        public static TaskCommentDto of(TaskCommentEntity comment, UserEntity author) {
            return new TaskCommentDto(comment.getId(), comment.getTaskId(), comment.getAuthorId(),
                    author == null ? null : UserDto.of(author), comment.getContent(), comment.getCreatedAt());
        }
    }
    public record CreateCommentReq(String content) {}
}
