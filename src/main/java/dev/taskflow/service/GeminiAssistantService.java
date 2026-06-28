package dev.taskflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import dev.taskflow.api.Dtos;
import dev.taskflow.domain.ChatMessageEntity;
import dev.taskflow.repo.ChatMessageRepository;
import dev.taskflow.repo.UserRepository;
import dev.taskflow.ws.ChatSocketRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GeminiAssistantService {
    private static final String ASSISTANT_USERNAME = "tasko_ai";

    private final ChatMessageRepository messages;
    private final UserRepository users;
    private final ChatSocketRegistry sockets;
    private final RestClient client;
    private final String apiKey;
    private final String model;

    public GeminiAssistantService(ChatMessageRepository messages,
                                  UserRepository users,
                                  ChatSocketRegistry sockets,
                                  @Value("${gemini.api-key:}") String apiKey,
                                  @Value("${gemini.model:gemini-2.5-flash}") String model) {
        this.messages = messages;
        this.users = users;
        this.sockets = sockets;
        this.apiKey = apiKey;
        this.model = model;
        this.client = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    @Async
    public void answerMention(UUID groupId, UUID sourceMessageId) {
        var source = messages.findById(sourceMessageId).orElse(null);
        if (source == null || source.getContent() == null) return;
        String content = source.getContent().toLowerCase();
        if (!content.contains("@" + ASSISTANT_USERNAME) && !content.contains("@flowa")) return;
        var bot = users.findByUsername(ASSISTANT_USERNAME)
                .or(() -> users.findByUsername("flowa")).orElse(null);
        if (bot == null) return;

        String answer;
        try {
            if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("Gemini key missing");
            var history = messages.findByGroupIdOrderByCreatedAtAsc(groupId);
            int from = Math.max(0, history.size() - 12);
            StringBuilder context = new StringBuilder();
            for (var message : history.subList(from, history.size())) {
                var sender = users.findById(message.getSenderId()).orElse(null);
                String name = sender == null ? "User" : sender.getUsername();
                if (message.getContent() != null && !message.getContent().isBlank()) {
                    context.append(name).append(": ").append(message.getContent()).append("\n");
                }
            }
            String prompt = """
                    You are Tasko AI, a concise and helpful assistant inside a team project chat.
                    Answer in the language used by the person who mentioned you.
                    Use the recent conversation as context. Do not repeat the @tasko_ai mention.
                    Keep the answer practical and reasonably short.

                    Recent conversation:
                    """ + context;
            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of("temperature", 0.5, "maxOutputTokens", 800)
            );
            JsonNode response = client.post()
                    .uri("/v1beta/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            answer = response == null ? "" : response.path("candidates").path(0)
                    .path("content").path("parts").path(0).path("text").asText();
            if (answer.isBlank()) answer = "Bu suala hazırda cavab yarada bilmədim.";
        } catch (Exception ignored) {
            answer = "Tasko AI hazırda cavab verə bilmir. Gemini konfiqurasiyasını yoxlayın.";
        }

        var saved = messages.save(ChatMessageEntity.builder()
                .groupId(groupId)
                .senderId(bot.getId())
                .content(answer.trim())
                .type(ChatMessageEntity.Type.TEXT)
                .replyToId(sourceMessageId)
                .build());
        var sourceSender = users.findById(source.getSenderId()).orElse(null);
        var sourceDto = new Dtos.ChatMessageDto(source.getId(), source.getGroupId(), source.getSenderId(),
                sourceSender == null ? null : Dtos.UserDto.of(sourceSender), source.getContent(),
                source.getType().name(), source.getMeetRoomId(), source.getMediaUrl(),
                source.getMediaName(), source.getReplyToId(), null,
                source.getMentionIds(), source.getCreatedAt());
        var dto = new Dtos.ChatMessageDto(saved.getId(), saved.getGroupId(), saved.getSenderId(),
                Dtos.UserDto.of(bot), saved.getContent(), saved.getType().name(), null,
                null, null, saved.getReplyToId(), sourceDto,
                saved.getMentionIds(), saved.getCreatedAt());
        sockets.broadcast(groupId, new Dtos.SocketEvent("message", dto));
    }
}
