
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class SecondRedisConfig {
    //元数据的redisTemplate，用于查询数据资产系统的元数据
    @Bean(name = "secondLettuceConnectionFactory")
    public LettuceConnectionFactory secondLettuceConnectionFactory() {
        LettuceClientConfiguration clientConfig =
                LettucePoolingClientConfiguration.builder().commandTimeout(Duration.ofMillis(timeout))
                        .poolConfig(secondPoolConfig()).build();
        return new LettuceConnectionFactory(secondRedisConfig(), clientConfig);
    }

    @Bean(name = "redisTemplate2")
    public RedisTemplate<String, Object> secondRedisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        /**
         * value 序列化
         */
        JdkSerializationRedisSerializer jdkSerializationRedisSerializer = new JdkSerializationRedisSerializer();

        // value值的序列化采用jdkSerializationRedisSerializer
        template.setValueSerializer(jdkSerializationRedisSerializer);
        template.setHashValueSerializer(jdkSerializationRedisSerializer);

        // key的序列化采用StringRedisSerializer
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setConnectionFactory(secondLettuceConnectionFactory());
        return template;
    }

    @Value("${spring.redis2.host:127.0.0.1}")
    private String host;
    @Value("${spring.redis2.port:6379}")
    private Integer port;
    @Value("${spring.redis2.password:}")
    private String password;
    @Value("${spring.redis2.database:0}")
    private Integer database;
    @Value("${spring.redis2.timeout:10000}")
    private Long timeout;

    @Value("${spring.redis2.lettuce.pool.max-active:8}")
    private Integer maxActive;
    @Value("${spring.redis2.lettuce.pool.max-idle:8}")
    private Integer maxIdle;
    @Value("${spring.redis2.lettuce.pool.max-wait:-1}")
    private Long maxWait;
    @Value("${spring.redis2.lettuce.pool.min-idle:0}")
    private Integer minIdle;

    public GenericObjectPoolConfig secondPoolConfig() {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(maxActive);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        config.setMaxWaitMillis(maxWait);
        return config;
    }

    public RedisStandaloneConfiguration secondRedisConfig() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPassword(RedisPassword.of(password));
        config.setPort(port);
        config.setDatabase(database);
        return config;
    }
}
