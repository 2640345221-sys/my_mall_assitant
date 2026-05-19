package assitant.memory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RedisChatMemory implements ChatMemory {

    private static final Logger log = LoggerFactory.getLogger(RedisChatMemory.class);
    private static final String KEY_PREFIX = "chat:memory:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisChatMemory(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public void add(String conversationId, Message message) {
        try {
            MessageEntry entry = MessageEntry.from(message);
            if (entry == null) return;
            String redisKey = key(conversationId);
            String json = objectMapper.writeValueAsString(entry);
            redis.opsForList().rightPush(redisKey, json);
            redis.expire(redisKey, java.time.Duration.ofDays(7));
        } catch (JsonProcessingException e) {
            log.error("序列化消息失败", e);
        }
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        for (Message message : messages) {
            add(conversationId, message);
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        List<String> jsons = redis.opsForList().range(key(conversationId), 0, -1);
        if (jsons == null || jsons.isEmpty()) {
            return List.of();
        }
        List<Message> messages = new ArrayList<>();
        for (String json : jsons) {
            try {
                MessageEntry entry = objectMapper.readValue(json, MessageEntry.class);
                Message msg = entry.toMessage();
                if (msg != null) {
                    messages.add(msg);
                }
            } catch (JsonProcessingException e) {
                log.error("反序列化消息失败", e);
            }
        }
        return messages;
    }

    @Override
    public void clear(String conversationId) {
        redis.delete(key(conversationId));
    }

    private String key(String conversationId) {
        return KEY_PREFIX + conversationId;
    }

    /**
     * 内部存储 DTO：只保留 type + text，避免 Jackson 多态反序列化问题
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class MessageEntry {
        @JsonProperty("t")
        private String type;
        @JsonProperty("c")
        private String text;

        static MessageEntry from(Message msg) {
            if (msg == null) return null;
            String type = msg.getMessageType() != null ? msg.getMessageType().getValue() : "UNKNOWN";
            return new MessageEntry(type, msg.getText());
        }

        Message toMessage() {
            if (type == null) return null;
            return switch (type.toUpperCase()) {
                case "USER" -> new UserMessage(text != null ? text : "");
                case "ASSISTANT" -> new AssistantMessage(text != null ? text : "");
                case "SYSTEM" -> new SystemMessage(text != null ? text : "");
                default -> null;
            };
        }
    }
}
