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

import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatGroupRepository groups;
    private final ChatMessageRepository messages;
    private final UserRepository users;
    private final ChatSocketRegistry sockets;
    private final GeminiAssistantService assistant;

    public ChatController(ChatGroupRepository groups, ChatMessageRepository messages,
                          UserRepository users, ChatSocketRegistry sockets,
                          GeminiAssistantService assistant) {
        this.groups = groups; this.messages = messages; this.users = users; this.sockets = sockets;
        this.assistant = assistant;
    }

    @GetMapping("/groups")
    public List<ChatGroupDto> myGroups() {
        var me = CurrentUser.require();
        return groups.findAllForUser(me.id()).stream().map(ChatGroupDto::of).toList();
    }

    @PostMapping("/groups")
    public ChatGroupDto create(@RequestBody CreateGroupReq req) {
        var me = CurrentUser.require();
        Set<UUID> members = new HashSet<>();
        members.add(me.id());
        if (req.memberIds() != null) members.addAll(req.memberIds());
        users.findByUsername("tasko_ai").ifPresent(bot -> members.add(bot.getId()));
        var g = groups.save(ChatGroupEntity.builder()
                .name(req.name())
                .direct(false)
                .memberIds(members)
                .build());
        return ChatGroupDto.of(g);
    }

    @PutMapping("/groups/{id}")
    public ChatGroupDto update(@PathVariable UUID id, @RequestBody UpdateGroupReq req) {
        var me = CurrentUser.require();
        var group = groups.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        if (!group.getMemberIds().contains(me.id())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (req.name() != null && !req.name().isBlank()) group.setName(req.name().trim());
        if (req.description() != null) group.setDescription(req.description().trim());
        if (req.avatarUrl() != null) group.setAvatarUrl(req.avatarUrl());
        if (req.memberIds() != null) {
            Set<UUID> memberIds = new HashSet<>(req.memberIds());
            memberIds.add(me.id());
            users.findByUsername("tasko_ai").ifPresent(bot -> memberIds.add(bot.getId()));
            group.setMemberIds(memberIds);
        }
        return ChatGroupDto.of(groups.save(group));
    }

    @GetMapping("/groups/{id}/messages")
    public List<ChatMessageDto> list(@PathVariable UUID id) {
        var me = CurrentUser.require();
        var g = groups.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!g.getMemberIds().contains(me.id())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return messages.findByGroupIdOrderByCreatedAtAsc(id).stream()
                .map(m -> toDto(m, users.findById(m.getSenderId()).orElse(null)))
                .toList();
    }

    @PostMapping("/groups/{id}/messages")
    public ChatMessageDto send(@PathVariable UUID id, @RequestBody SendMessageReq req) {
        var me = CurrentUser.require();
        var g = groups.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!g.getMemberIds().contains(me.id())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        ChatMessageEntity.Type type = ChatMessageEntity.Type.TEXT;
        if (req.type() != null) { try { type = ChatMessageEntity.Type.valueOf(req.type()); } catch (Exception ignored) {} }
        var saved = messages.save(ChatMessageEntity.builder()
                .groupId(id)
                .senderId(me.id())
                .content(req.content())
                .type(type)
                .meetRoomId(req.meetRoomId())
                .mediaUrl(req.mediaUrl())
                .mediaName(req.mediaName())
                .replyToId(req.replyToId())
                .build());
        var dto = toDto(saved, users.findById(me.id()).orElse(null));
        sockets.broadcast(id, new SocketEvent("message", dto));
        assistant.answerMention(id, saved.getId());
        return dto;
    }

    private ChatMessageDto toDto(ChatMessageEntity m, UserEntity sender) {
        UserDto s = sender == null ? null : UserDto.of(sender);
        ChatMessageDto reply = null;
        if (m.getReplyToId() != null) {
            var original = messages.findById(m.getReplyToId()).orElse(null);
            if (original != null) {
                var originalSender = users.findById(original.getSenderId()).orElse(null);
                reply = new ChatMessageDto(original.getId(), original.getGroupId(), original.getSenderId(),
                        originalSender == null ? null : UserDto.of(originalSender), original.getContent(),
                        original.getType().name(), original.getMeetRoomId(), original.getMediaUrl(),
                        original.getMediaName(), original.getReplyToId(), null, original.getCreatedAt());
            }
        }
        return new ChatMessageDto(m.getId(), m.getGroupId(), m.getSenderId(), s,
                m.getContent(), m.getType().name(), m.getMeetRoomId(), m.getMediaUrl(),
                m.getMediaName(), m.getReplyToId(), reply, m.getCreatedAt());
    }
}
