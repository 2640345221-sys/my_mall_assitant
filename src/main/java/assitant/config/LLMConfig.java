package assitant.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LLMConfig {
    @Resource
    private ChatMemory chatMemory;

    @Bean
    public ChatClient chatClient(DashScopeChatModel dashScopeChatModel) {
        return ChatClient.builder(dashScopeChatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultOptions(ChatOptions.builder().temperature(0.3).build())
                .build();
    }
}
