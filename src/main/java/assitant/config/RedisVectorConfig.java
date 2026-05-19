package assitant.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

@Configuration
public class RedisVectorConfig {

    @Bean
    public JedisPooled jedisPooled(RedisProperties props) {
        var config = DefaultJedisClientConfig.builder()
                .password(props.getPassword())
                .database(0)
                .build();
        return new JedisPooled(new HostAndPort(props.getHost(), props.getPort()), config);
    }

    @Bean(name = "goodsVectorStore")
    public VectorStore goodsVectorStore(JedisPooled jedisPooled,
                                   @Qualifier("dashscopeEmbeddingModel") EmbeddingModel embeddingModel) {
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName("idx:goods_rag")
                .prefix("goods:rag:")
                .initializeSchema(true)
                .build();
    }

    @Bean(name = "faqVectorStore")
    public VectorStore faqVectorStore(JedisPooled jedisPooled,
                                   @Qualifier("dashscopeEmbeddingModel") EmbeddingModel embeddingModel) {
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName("idx:faq")
                .prefix("faq:")
                .initializeSchema(true)
                .build();
    }
}
