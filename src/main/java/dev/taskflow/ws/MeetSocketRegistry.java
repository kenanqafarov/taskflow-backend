package dev.taskflow.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MeetSocketRegistry {
    private final Map<String, Map<String, WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;

    public MeetSocketRegistry(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void register(String roomId, String connectionId, WebSocketSession session,
                         String userId, String username) {
        var room = rooms.computeIfAbsent(roomId, key -> new ConcurrentHashMap<>());
        var existing = room.entrySet().stream()
                .map(entry -> participant(entry.getKey(), entry.getValue()))
                .toList();
        session.getAttributes().put("roomId", roomId);
        session.getAttributes().put("connectionId", connectionId);
        session.getAttributes().put("userId", userId);
        session.getAttributes().put("username", username);
        room.put(connectionId, session);
        send(session, Map.of("type", "participants", "data", existing));
        broadcastExcept(roomId, connectionId, Map.of(
                "type", "participant-joined",
                "data", participant(connectionId, session)
        ));
    }

    public void unregister(WebSocketSession session) {
        String roomId = (String) session.getAttributes().get("roomId");
        String connectionId = (String) session.getAttributes().get("connectionId");
        if (roomId == null || connectionId == null) return;
        var room = rooms.get(roomId);
        if (room == null) return;
        room.remove(connectionId);
        broadcastExcept(roomId, connectionId, Map.of(
                "type", "participant-left",
                "data", Map.of("id", connectionId)
        ));
        if (room.isEmpty()) rooms.remove(roomId);
    }

    public void relay(String roomId, String target, Map<String, Object> payload) {
        var room = rooms.get(roomId);
        if (room != null) send(room.get(target), payload);
    }

    private void broadcastExcept(String roomId, String excluded, Map<String, Object> payload) {
        var room = rooms.get(roomId);
        if (room == null) return;
        room.forEach((id, session) -> {
            if (!id.equals(excluded)) send(session, payload);
        });
    }

    private Map<String, String> participant(String id, WebSocketSession session) {
        return Map.of(
                "id", id,
                "userId", String.valueOf(session.getAttributes().get("userId")),
                "name", String.valueOf(session.getAttributes().get("username"))
        );
    }

    private void send(WebSocketSession session, Map<String, Object> payload) {
        if (session == null || !session.isOpen()) return;
        try {
            String json = mapper.writeValueAsString(payload);
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (Exception ignored) {}
    }
}
