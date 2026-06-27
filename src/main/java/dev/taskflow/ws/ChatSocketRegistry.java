package dev.taskflow.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.taskflow.api.Dtos.SocketEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.socket.TextMessage;

@Component
public class ChatSocketRegistry {
    private final Map<UUID, Set<WebSocketSession>> byGroup = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;

    public ChatSocketRegistry(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void register(UUID groupId, WebSocketSession session) {
        byGroup.computeIfAbsent(groupId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(UUID groupId, WebSocketSession session) {
        var set = byGroup.get(groupId);
        if (set != null) set.remove(session);
    }

    public void broadcast(UUID groupId, SocketEvent event) {
        var set = byGroup.get(groupId);
        if (set == null) return;
        String payload;
        try { payload = mapper.writeValueAsString(event); } catch (Exception e) { return; }
        for (WebSocketSession s : set) {
            if (!s.isOpen()) continue;
            try {
                synchronized (s) { s.sendMessage(new TextMessage(payload)); }
            } catch (IOException ignored) {}
        }
    }
}
