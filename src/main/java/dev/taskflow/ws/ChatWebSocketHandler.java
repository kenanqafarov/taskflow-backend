package dev.taskflow.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.taskflow.api.Dtos.SocketEvent;
import dev.taskflow.domain.ChatGroupEntity;
import dev.taskflow.repo.ChatGroupRepository;
import dev.taskflow.security.JwtService;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final JwtService jwt;
    private final ChatSocketRegistry registry;
    private final ChatGroupRepository groups;
    private final ObjectMapper mapper;

    public ChatWebSocketHandler(JwtService jwt, ChatSocketRegistry registry,
                                ChatGroupRepository groups, ObjectMapper mapper) {
        this.jwt = jwt; this.registry = registry; this.groups = groups; this.mapper = mapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Map<String, String> q = parseQuery(session.getUri());
        String token = q.get("token");
        String group = q.get("group");
        if (token == null || group == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        try {
            JwtService.Claims claims = jwt.parse(token);
            UUID groupId = UUID.fromString(group);
            ChatGroupEntity chatGroup = groups.findById(groupId).orElse(null);
            if (chatGroup == null || !chatGroup.getMemberIds().contains(claims.userId())) {
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }
            session.getAttributes().put("groupId", groupId);
            session.getAttributes().put("userId", claims.userId());
            session.getAttributes().put("username", claims.username());
            registry.register(groupId, session);
        } catch (Exception e) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, org.springframework.web.socket.TextMessage message) {
        try {
            var node = mapper.readTree(message.getPayload());
            if (!"typing".equals(node.path("event").asText())) return;
            UUID groupId = (UUID) session.getAttributes().get("groupId");
            Map<String, Object> data = new HashMap<>();
            data.put("userId", session.getAttributes().get("userId"));
            data.put("username", session.getAttributes().get("username"));
            data.put("typing", node.path("typing").asBoolean());
            registry.broadcast(groupId, new SocketEvent("typing", data));
        } catch (Exception ignored) {}
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID groupId = (UUID) session.getAttributes().get("groupId");
        if (groupId != null) registry.unregister(groupId, session);
    }

    private static Map<String, String> parseQuery(URI uri) {
        Map<String, String> out = new HashMap<>();
        if (uri == null || uri.getQuery() == null) return out;
        for (String pair : uri.getQuery().split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) out.put(java.net.URLDecoder.decode(pair.substring(0, eq), java.nio.charset.StandardCharsets.UTF_8),
                                java.net.URLDecoder.decode(pair.substring(eq + 1), java.nio.charset.StandardCharsets.UTF_8));
        }
        return out;
    }
}
