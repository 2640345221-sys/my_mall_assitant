package assitant.controller;

import assitant.agent.MallAgentService;
import assitant.entity.dto.ChatRequest;
import assitant.entity.dto.ChatResponse;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Resource
    private MallAgentService agentService;


    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String cid = request.getConversationId() != null ? request.getConversationId() : "default";
        String reply = agentService.chat(request.getUserId(), cid, request.getMessage());
        ChatResponse response = new ChatResponse();
        response.setReply(reply);
        return response;
    }

    @PostMapping(path = "/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        String cid = request.getConversationId() != null ? request.getConversationId() : "default";
        SseEmitter emitter = new SseEmitter(0L);
        agentService.chatStream(request.getUserId(), cid, request.getMessage())
                .subscribe(
                        token -> { try { emitter.send(SseEmitter.event().data(token)); } catch (Exception ignored) {} },
                        emitter::completeWithError,
                        emitter::complete
                );
        return emitter;
    }
}
