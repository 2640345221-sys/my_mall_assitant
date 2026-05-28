package assitant.agent;

import assitant.annotation.OperationLog;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Objects;

@Slf4j
@Service
public class MallAgentService {

    @Resource
    private ReactAgent mallReactAgent;

    @OperationLog(module = "Agent", type = "对话", description = "同步聊天", recordResult = true)
    public String chat(String userId, String conversationId, String message) {
        try {
            Message lastMsg = mallReactAgent.streamMessages(
                            new UserMessage("用户ID: " + userId + "\n用户消息: " + message),
                            RunnableConfig.builder().threadId(conversationId).build())
                    .filter(m -> m instanceof AssistantMessage)
                    .blockLast();
            if (lastMsg instanceof AssistantMessage am) {
                String text = am.getText();
                return text != null ? text : "";
            }
        } catch (Exception e) {
            log.error("[Agent] chat error", e);
        }
        return "抱歉，处理出错了，请稍后重试。";
    }

    @OperationLog(module = "Agent", type = "对话", description = "流式聊天", recordResult = true)
    public Flux<String> chatStream(String userId, String conversationId, String message) {
        try {
            return mallReactAgent.streamMessages(
                            new UserMessage("用户ID: " + userId + "\n用户消息: " + message),
                            RunnableConfig.builder().threadId(conversationId).build())
                    .filter(m -> m instanceof AssistantMessage)
                    .map(m -> ((AssistantMessage) m).getText())
                    .filter(Objects::nonNull)
                    .doOnError(e -> log.error("[Agent] stream error", e));
        } catch (Exception e) {
            log.error("[Agent] chatStream error", e);
            return Flux.error(e);
        }
    }
}
