package assitant.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class OpenAiRequest {
    private String model;
    private List<Message> messages;
    private Boolean stream;
    private String user;

    @Data
    public static class Message {
        private String role;
        private String content;
        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;
    }

    @Data
    public static class ToolCall {
        private String id;
        private String type;
        private Function function;
    }

    @Data
    public static class Function {
        private String name;
        private String arguments;
    }
}
