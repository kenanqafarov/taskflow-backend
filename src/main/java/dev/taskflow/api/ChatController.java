package dev.taskflow.api;

import dev.taskflow.api.Dtos.*;
import dev.taskflow.domain.*;
import dev.taskflow.repo.*;
import dev.taskflow.security.CurrentUser;
import dev.taskflow.service.GeminiAssistantService;
import dev.taskflow.ws.ChatSocketRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatGroupRepository groups;
    private final ChatMessageRepository messages;
    private final UserRepository users;
    private final ChatSocketRegistry sockets;
    private final GeminiAssistantService assistant;
    private final ChatReadStateRepository readStates;
    private final NotificationRepository notifications;

    public ChatController(ChatGroupRepository groups, ChatMessageRepository messages,
                          UserRepository users, ChatSocketRegistry sockets,
                          GeminiAssistantService assistant, ChatReadStateRepository readStates,
                          NotificationRepository notifications) {
        this.groups = groups;
        this.messages = messages;
        this.users = users;
        this.sockets = sockets;
        this.assistant = assistant;
        this.readStates = readStates;
        this.notifications = notifications;
    }

    @GetMapping("/groups")
    public List<ChatGroupDto> myGroups() {
        var me = CurrentUser.require();
        return groups.findAllForUser(me.id()).stream().map(group -> toGroupDto(group, me.id())).toList();
    }

    @PostMapping("/groups")
    public ChatGroupDto create(@RequestBody CreateGroupReq req) {
        var me = CurrentUser.require();
        if (req.name() == null || req.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group name is required");
        }
        Set<UUID> members = new HashSet<>();
        members.add(me.id());
        if (req.memberIds() != null) members.addAll(req.memberIds());
        findAssistant().ifPresent(bot -> members.add(bot.getId()));
        var group = groups.save(ChatGroupEntity.builder()
                .name(req.name().trim())
                .description(req.description())
                .avatarUrl(req.avatarUrl())
                .direct(false)
                .memberIds(members)
                .build());
        return toGroupDto(group, me.id());
    }

    @PutMapping("/groups/{id}")
    public ChatGroupDto update(@PathVariable UUID id, @RequestBody UpdateGroupReq req) {
        var me = CurrentUser.require();
        if (!me.isAdmin()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only");
        var group = groups.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        if (req.name() != null && !req.name().isBlank()) group.setName(req.name().trim());
        if (req.description() != null) group.setDescription(req.description().trim());
        if (req.avatarUrl() != null) group.setAvatarUrl(req.avatarUrl());
        if (req.memberIds() != null) {
            Set<UUID> memberIds = new HashSet<>(req.memberIds());
            findAssistant().ifPresent(bot -> memberIds.add(bot.getId()));
            group.setMemberIds(memberIds);
        }
        return toGroupDto(groups.save(group), me.id());
    }

    @GetMapping("/groups/{id}/messages")
    public List<ChatMessageDto> list(@PathVariable UUID id) {
        var me = requireMember(id);
        return messages.findByGroupIdOrderByCreatedAtAsc(id).stream()
                .map(message -> toDto(message, users.findById(message.getSenderId()).orElse(null)))
                .toList();
    }

    @GetMapping("/groups/{id}/search")
    public List<ChatMessageDto> search(@PathVariable UUID id, @RequestParam String q) {
        requireMember(id);
        String query = q == null ? "" : q.trim().toLowerCase();
        return messages.findByGroupIdOrderByCreatedAtAsc(id).stream()
                .filter(message -> message.getContent() != null && message.getContent().toLowerCase().contains(query))
                .map(message -> toDto(message, users.findById(message.getSenderId()).orElse(null)))
                .toList();
    }

    @PatchMapping("/groups/{id}/read")
    public ChatGroupDto markRead(@PathVariable UUID id, @RequestBody MarkChatReadReq req) {
        var me = requireMember(id);
        var group = groups.findById(id).orElseThrow();
        var state = readStates.findByGroupIdAndUserId(id, me.id())
                .orElseGet(() -> ChatReadStateEntity.builder().groupId(id).userId(me.id()).build());
        state.setLastReadMessageId(req.lastReadMessageId());
        state.setScrollMessageId(req.scrollMessageId());
        state.setLastReadAt(Instant.now());
        readStates.save(state);
        return toGroupDto(group, me.id());
    }

    @PostMapping("/groups/{id}/messages")
    public ChatMessageDto send(@PathVariable UUID id, @RequestBody SendMessageReq req) {
        var me = requireMember(id);
        var group = groups.findById(id).orElseThrow();
        ChatMessageEntity.Type type = ChatMessageEntity.Type.TEXT;
        if (req.type() != null) {
            try { type = ChatMessageEntity.Type.valueOf(req.type()); } catch (Exception ignored) {}
        }
        Set<UUID> mentionIds = new HashSet<>();
        if (req.content() != null) {
            var matcher = Pattern.compile("@([a-zA-Z0-9_.-]+)").matcher(req.content());
            while (matcher.find()) users.findByUsername(matcher.group(1))
                    .ifPresent(user -> mentionIds.add(user.getId()));
        }
        var saved = messages.save(ChatMessageEntity.builder()
                .groupId(id).senderId(me.id()).content(req.content()).type(type)
                .meetRoomId(req.meetRoomId()).mediaUrl(req.mediaUrl()).mediaName(req.mediaName())
                .replyToId(req.replyToId()).mentionIds(mentionIds).build());
        var dto = toDto(saved, users.findById(me.id()).orElse(null));
        sockets.broadcast(id, new SocketEvent("message", dto));
        mentionIds.stream().filter(userId -> !userId.equals(me.id()))
                .filter(userId -> users.findById(userId).map(user -> !Boolean.FALSE.equals(user.getNotificationsEnabled())).orElse(false))
                .forEach(userId ->
                notifications.save(NotificationEntity.builder()
                        .userId(userId).text(me.username() + " mentioned you in #" + group.getName())
                        .type("MENTION").resourceId(id)
                        .actionUrl("/chat?group=" + id + "&message=" + saved.getId()).build()));
        assistant.answerMention(id, saved.getId());
        return dto;
    }

    private dev.taskflow.security.AuthPrincipal requireMember(UUID groupId) {
        var me = CurrentUser.require();
        var group = groups.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        if (!group.getMemberIds().contains(me.id())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return me;
    }

    private Optional<UserEntity> findAssistant() {
        return users.findByUsername("tasko_ai").or(() -> users.findByUsername("flowa"));
    }

    private ChatGroupDto toGroupDto(ChatGroupEntity group, UUID userId) {
        var state = readStates.findByGroupIdAndUserId(group.getId(), userId).orElse(null);
        Instant after = state == null || state.getLastReadAt() == null ? Instant.EPOCH : state.getLastReadAt();
        return new ChatGroupDto(group.getId(), group.getName(), group.getDescription(), group.getAvatarUrl(),
                group.isDirect(), group.getMemberIds(),
                messages.countByGroupIdAndCreatedAtAfterAndSenderIdNot(group.getId(), after, userId),
                messages.countMentionsAfter(group.getId(), userId, after),
                state == null ? null : state.getLastReadMessageId(),
                state == null ? null : state.getScrollMessageId(), group.getCreatedAt());
    }

    private ChatMessageDto toDto(ChatMessageEntity message, UserEntity sender) {
        ChatMessageDto reply = null;
        if (message.getReplyToId() != null) {
            var original = messages.findById(message.getReplyToId()).orElse(null);
            if (original != null) {
                var originalSender = users.findById(original.getSenderId()).orElse(null);
                reply = new ChatMessageDto(original.getId(), original.getGroupId(), original.getSenderId(),
                        originalSender == null ? null : UserDto.of(originalSender), original.getContent(),
                        original.getType().name(), original.getMeetRoomId(), original.getMediaUrl(),
                        original.getMediaName(), original.getReplyToId(), null,
                        original.getMentionIds(), original.getCreatedAt());
            }
        }
        return new ChatMessageDto(message.getId(), message.getGroupId(), message.getSenderId(),
                sender == null ? null : UserDto.of(sender), message.getContent(), message.getType().name(),
                message.getMeetRoomId(), message.getMediaUrl(), message.getMediaName(),
                message.getReplyToId(), reply, message.getMentionIds(), message.getCreatedAt());
    }
}
