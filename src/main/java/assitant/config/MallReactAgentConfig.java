package assitant.config;

import assitant.agent.tool.*;
import assitant.utils.SystemPromptLoader;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;
import com.alibaba.cloud.ai.graph.agent.hook.toolcalllimit.ToolCallLimitHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MallReactAgentConfig {

    @Bean
    public RedissonClient redissonClient(RedisProperties props) {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + props.getHost() + ":" + props.getPort())
                .setPassword(props.getPassword())
                .setDatabase(props.getDatabase());
        return Redisson.create(config);
    }

    @Bean
    public RedisSaver redisSaver(RedissonClient redissonClient) {
        return RedisSaver.builder()
                .redisson(redissonClient)
                .build();
    }

    @Bean
    public SummarizationHook summarizationHook(ChatModel chatModel) {
        return SummarizationHook.builder()
                .model(chatModel)
                .maxTokensBeforeSummary(16000)
                .messagesToKeep(20)
                .build();
    }

    @Bean
    public ToolCallLimitHook toolCallLimitHook() {
        return ToolCallLimitHook.builder()
                .runLimit(10)
                .build();
    }

    @Bean
    public ModelCallLimitHook modelCallLimitHook() {
        return ModelCallLimitHook.builder()
                .runLimit(15)
                .build();
    }

    @Bean
    public ReactAgent mallReactAgent(
            ChatModel chatModel,
            RedisSaver redisSaver,
            SystemPromptLoader promptLoader,
            SummarizationHook summarizationHook,
            ToolCallLimitHook toolCallLimitHook,
            ModelCallLimitHook modelCallLimitHook,
            GoodsTool goodsTool,
            CartTool cartTool,
            OrderTool orderTool,
            UserTool userTool,
            SeckillGoodsTool seckillGoodsTool,
            FaqTool faqTool,
            ReplyTool replyTool) {
        log.info("[Agent] 初始化 ReactAgent");
        return ReactAgent.builder()
                .name("mall-assistant")
                .model(chatModel)
                .instruction(promptLoader.getSystemPrompt())
                .enableLogging(true)
                .saver(redisSaver)
                .methodTools(goodsTool, cartTool, orderTool,
                        userTool, seckillGoodsTool, faqTool, replyTool)
                .hooks(summarizationHook, toolCallLimitHook, modelCallLimitHook)
                .build();
    }
}
