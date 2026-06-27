package dev.taskflow.api;

import dev.taskflow.domain.UserEntity;
import dev.taskflow.domain.TaskEntity;
import dev.taskflow.domain.BoardEntity;
import dev.taskflow.domain.BoardColumnEntity;
import dev.taskflow.domain.ChatGroupEntity;
import dev.taskflow.domain.ChatMessageEntity;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class Dtos {
    private Dtos() {}

    public record UserDto(UUID id, String username, String firstName, String lastName,
                          String email, String githubUrl, String linkedinUrl, String avatarUrl,
                          String role, Instant createdAt) {
        public static UserDto of(UserEntity u) {
            return new UserDto(u.getId(), u.getUsername(), u.getFirstName(), u.getLastName(),
                    u.getEmail(), u.getGithubUrl(), u.getLinkedinUrl(), u.getAvatarUrl(),
                    u.getRole().name(), u.getCreatedAt());
        }
    }

    public record LoginReq(String username, String password) {}
    public record LoginRes(String token, UserDto user) {}

    public record CreateUserReq(String username, String password, String firstName, String lastName,
                                String email, String githubUrl, String linkedinUrl, String role) {}
    public record UpdateUserReq(String username, String password, String firstName, String lastName,
                                String email, String githubUrl, String linkedinUrl, String avatarUrl, String role) {}

    public record BoardDto(UUID id, String name, String description, UUID ownerId, Instant createdAt) {
        public static BoardDto of(BoardEntity b) {
            return new BoardDto(b.getId(), b.getName(), b.getDescription(), b.getOwnerId(), b.getCreatedAt());
        }
    }
    public record CreateBoardReq(String name, String description) {}

    public record ColumnDto(UUID id, UUID boardId, String name, int position) {
        public static ColumnDto of(BoardColumnEntity c) {
            return new ColumnDto(c.getId(), c.getBoardId(), c.getName(), c.getPosition());
        }
    }
    public record CreateColumnReq(String name) {}
    public record UpdateColumnReq(String name) {}
    public record MoveColumnReq(Integer position) {}

    public record TaskDto(UUID id, UUID boardId, UUID columnId, String title, String description,
                          int position, String priority, UUID assigneeId, Instant dueDate, Instant createdAt) {
        public static TaskDto of(TaskEntity t) {
            return new TaskDto(t.getId(), t.getBoardId(), t.getColumnId(), t.getTitle(), t.getDescription(),
                    t.getPosition(), t.getPriority().name(), t.getAssigneeId(), t.getDueDate(), t.getCreatedAt());
        }
    }
    public record CreateTaskReq(UUID columnId, String title, String description, String priority, UUID assigneeId) {}
    public record UpdateTaskReq(String title, String description, String priority, UUID assigneeId, Instant dueDate) {}
    public record MoveTaskReq(UUID columnId, Integer position) {}
    public record AssignTaskReq(UUID assigneeId) {}

    public record BoardDetail(BoardDto board, List<ColumnDto> columns, List<TaskDto> tasks) {}

    public record ChatGroupDto(UUID id, String name, String description, String avatarUrl,
                               boolean isDirect, Set<UUID> memberIds, Instant createdAt) {
        public static ChatGroupDto of(ChatGroupEntity g) {
            return new ChatGroupDto(g.getId(), g.getName(), g.getDescription(), g.getAvatarUrl(),
                    g.isDirect(), g.getMemberIds(), g.getCreatedAt());
        }
    }
    public record CreateGroupReq(String name, List<UUID> memberIds) {}
    public record UpdateGroupReq(String name, String description, String avatarUrl, List<UUID> memberIds) {}

    public record ChatMessageDto(UUID id, UUID groupId, UUID senderId, UserDto sender,
                                 String content, String type, String meetRoomId, String mediaUrl,
                                 String mediaName, UUID replyToId, ChatMessageDto replyTo, Instant createdAt) {}
    public record SendMessageReq(String content, String type, String meetRoomId,
                                 String mediaUrl, String mediaName, UUID replyToId) {}
    public record SocketEvent(String event, Object data) {}

    public record StartMeetReq(UUID groupId) {}
    public record StartMeetRes(String roomId, String url) {}
    public record MeetTokenRes(String token, String url) {}
}
