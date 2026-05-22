package assitant.agent;

import assitant.agent.tool.*;
import assitant.utils.SystemPromptLoader;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class MallAgentService {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private GoodsTool goodsTool;
    @Autowired
    private CartTool cartTool;
    @Autowired
    private OrderTool orderTool;
    @Autowired
    private UserTool userTool;
    @Autowired
    private SeckillGoodsTool seckillGoodsTool;
    @Autowired
    private ReplyTool replyTool;
    @Autowired
    private FaqTool faqTool;
    @Autowired
    private SystemPromptLoader systemPromptLoader;

    private String systemPrompt;

    @PostConstruct
    public void init() {
        this.systemPrompt = systemPromptLoader.getSystemPrompt();
        log.info("[Agent] SystemPrompt 已加载, 长度={}", systemPrompt.length());
    }

    public String chat(String userId, String conversationId, String message) {
        log.info("[Agent] chat: userId={}, conversationId={}, message={}", userId, conversationId, message);
        String reply = chatClient.prompt()
                .system(systemPrompt)
                .user("用户ID: " + userId + "\n[强制规则]回复前必须检查可用工具并调用]\n用户消息: " + message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(goodsTool, cartTool, orderTool, userTool, seckillGoodsTool, faqTool, replyTool)
                .call()
                .content();
        log.info("[Agent] reply: {}", reply);
        return reply;
    }

    public Flux<String> chatStream(String userId, String conversationId, String message) {
        log.info("[Agent] chatStream: userId={}, conversationId={}, message={}", userId, conversationId, message);
        return chatClient.prompt()
                .system(systemPrompt)
                .user("用户ID: " + userId + "\n[强制规则]回复前必须检查可用工具并调用]\n用户消息: " + message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(goodsTool, cartTool, orderTool, userTool, seckillGoodsTool, faqTool, replyTool)
                .stream()
                .content()
                .doOnNext(token -> log.debug("[Agent] token: {}", token))
                .doOnComplete(() -> log.info("[Agent] stream complete"))
                .doOnError(e -> log.error("[Agent] stream error", e));
    }
}
