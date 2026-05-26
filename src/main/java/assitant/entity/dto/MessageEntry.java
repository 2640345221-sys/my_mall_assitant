package assitant.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageEntry {
    @JsonProperty("t")
    private String type;
    @JsonProperty("c")
    private String text;

    public static MessageEntry from(Message msg) {
        if (msg == null) return null;
        String type = msg.getMessageType() != null ? msg.getMessageType().getValue() : "UNKNOWN";
        return new MessageEntry(type, msg.getText());
    }

    public Message toMessage() {
        if (type == null) return null;
        return switch (type.toUpperCase()) {
            case "USER" -> new UserMessage(text != null ? text : "");
            case "ASSISTANT" -> new AssistantMessage(text != null ? text : "");
            case "SYSTEM" -> new SystemMessage(text != null ? text : "");
            default -> null;
        };
    }
}
