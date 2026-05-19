package assitant.entity.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String userId;
    private String message;
    private String conversationId;
}
