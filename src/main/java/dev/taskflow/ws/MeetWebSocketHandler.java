package dev.taskflow.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.taskflow.security.JwtService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class MeetWebSocketHandler extends TextWebSocketHandler {
    private final JwtService jwt;
    private final MeetSocketRegistry registry;
    private final ObjectMapper mapper;

    public MeetWebSocketHandler(JwtService jwt, MeetSocketRegistry registry, ObjectMapper mapper) {
        this.jwt = jwt;
        this.registry = registry;
        this.mapper = mapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            Map<String, String> query = parseQuery(session.getUri());
            var claims = jwt.parse(query.get("token"));
            String roomId = query.get("room");
            if (roomId == null || roomId.isBlank()) throw new IllegalArgumentException("Room required");
            String connectionId = UUID.randomUUID().toString();
            registry.register(roomId, connectionId, session, claims.userId().toString(), claims.username());
        } catch (Exception error) {
            session.close(CloseStatus.POLICY_VIOLATION);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode node = mapper.readTree(message.getPayload());
            String type = node.path("type").asText();
            if (!type.equals("offer") && !type.equals("answer") && !type.equals("ice")) return;
            String target = node.path("target").asText();
            if (target.isBlank()) return;
            String roomId = (String) session.getAttributes().get("roomId");
            String from = (String) session.getAttributes().get("connectionId");
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", type);
            payload.put("from", from);
            if (type.equals("ice")) payload.put("candidate", node.get("candidate"));
            else payload.put("sdp", node.get("sdp"));
            registry.relay(roomId, target, payload);
        } catch (Exception ignored) {}
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.unregister(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        registry.unregister(session);
        if (session.isOpen()) session.close(CloseStatus.SERVER_ERROR);
    }

    private static Map<String, String> parseQuery(URI uri) {
        Map<String, String> values = new HashMap<>();
        if (uri == null || uri.getQuery() == null) return values;
        for (String pair : uri.getQuery().split("&")) {
            int separator = pair.indexOf('=');
            if (separator > 0) {
                values.put(
                        URLDecoder.decode(pair.substring(0, separator), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8)
                );
            }
        }
        return values;
    }
}
