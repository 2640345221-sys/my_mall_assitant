package assitant.controller;

import assitant.agent.MallAgentService;
import assitant.entity.dto.OpenAiRequest;
import assitant.entity.dto.OpenAiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class OpenAiController {

    @Autowired
    private MallAgentService agentService;
    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/chat/completions")
    public Object chatCompletions(@RequestBody OpenAiRequest request) {
        String userMessage = extractUserMessage(request);
        String userId = request.getUser() != null ? request.getUser() : "anonymous";
        String model = request.getModel() != null ? request.getModel() : "deepseek";
        String conversationId = request.getUser() != null ? request.getUser() : "default";

        if (Boolean.TRUE.equals(request.getStream())) {
            return streamResponse(userId, conversationId, userMessage, model);
        }
        String reply = agentService.chat(userId, conversationId, userMessage);
        return OpenAiResponse.of(model, reply);
    }

    private Flux<String> streamResponse(String userId, String conversationId, String message, String model) {
        String chatId = "chatcmpl-" + UUID.randomUUID().toString().substring(0, 8);
        return agentService.chatStream(userId, conversationId, message)
                .map(token -> sseChunk(chatId, model, token));
    }

    private String sseChunk(String id, String model, String token) {
        try {
            String json = objectMapper.writeValueAsString(new SseChunk(id, model, token));
            return "data: " + json + "\n\n";
        } catch (JsonProcessingException e) {
            return "data: {}\n\n";
        }
    }

    private String extractUserMessage(OpenAiRequest request) {
        if (request.getMessages() == null) return "";
        for (int i = request.getMessages().size() - 1; i >= 0; i--) {
            OpenAiRequest.Message msg = request.getMessages().get(i);
            if ("user".equals(msg.getRole()) && msg.getContent() != null) {
                return msg.getContent();
            }
        }
        return request.getMessages().get(request.getMessages().size() - 1).getContent();
    }

    @Data
    @AllArgsConstructor
    private static class SseChunk {
        private String id;
        private String object = "chat.completion.chunk";
        private long created = System.currentTimeMillis() / 1000;
        private String model;
        private Choice[] choices;

        SseChunk(String id, String model, String token) {
            this.id = id;
            this.model = model;
            Choice choice = new Choice();
            choice.index = 0;
            choice.delta = new Delta();
            choice.delta.content = token;
            this.choices = new Choice[]{choice};
        }

        @Data
        static class Choice {
            private int index;
            private Delta delta;
        }

        @Data
        static class Delta {
            private String content;
        }
    }
}
