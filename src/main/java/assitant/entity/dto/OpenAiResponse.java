package assitant.entity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenAiResponse {
    private String id;
    private String object = "chat.completion";
    private long created;
    private String model;
    private List<Choice> choices;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Choice {
        private int index;
        private Message message;
        private Message delta;
        private String finishReason;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Message {
        private String role;
        private String content;
    }

    public static OpenAiResponse of(String model, String content) {
        OpenAiResponse resp = new OpenAiResponse();
        resp.id = "chatcmpl-" + UUID.randomUUID().toString().substring(0, 8);
        resp.created = System.currentTimeMillis() / 1000;
        resp.model = model;
        Message msg = new Message();
        msg.role = "assistant";
        msg.content = content;
        Choice choice = new Choice();
        choice.index = 0;
        choice.message = msg;
        choice.finishReason = "stop";
        resp.choices = List.of(choice);
        return resp;
    }
}
