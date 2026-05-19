package assitant.config;

import assitant.memory.RedisChatMemory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    @Bean
    public ChatMemory chatMemory(StringRedisTemplate stringRedisTemplate) {
        return new RedisChatMemory(stringRedisTemplate, objectMapper);
    }
}
