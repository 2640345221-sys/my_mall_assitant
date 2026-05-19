package assitant.config;

import assitant.agent.tool.*;
import assitant.utils.SystemPromptLoader;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

// TODO [修复] ReactAgent 未使用：该 Bean 已创建但 MallAgentService 仍用 ChatClient。要么删除此文件及 spring-ai-alibaba-agent-framework 依赖，要么将 MallAgentService 切换到 ReactAgent（注意 ReactAgent 无流式）
@Configuration
public class ReactAgentConfig {

    @Autowired
    private SystemPromptLoader promptLoader;
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

    @Bean
    public ReactAgent reactAgent(DashScopeChatModel chatModel) {
        return ReactAgent.builder()
                .name("mall_agent")
                .model(chatModel)
                .systemPrompt(promptLoader.getSystemPrompt())
                .methodTools(goodsTool, cartTool, orderTool, userTool, seckillGoodsTool)
                .saver(new MemorySaver())
                .build();
    }
}
