
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig extends CachingConfigurerSupport {

    /**
     * 自定义缓存key生成策略
     * 使用方法 @Cacheable(keyGenerator="keyGenerator")
     * @return
     */
    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getName());
            sb.append(method.getName());
            for (Object obj : params) {
                sb.append(JSON.toJSONString(obj).hashCode());
            }
            return sb.toString();
        };
    }

    /**
     *  设置 redis 数据默认过期时间，默认1天
     *  设置@cacheable 序列化方式
     * @return
     */
    @Bean
    public RedisCacheConfiguration redisCacheConfiguration(){
        FastJsonRedisSerializer<Object> fastJsonRedisSerializer = new FastJsonRedisSerializer<>(Object.class);
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig();
        configuration = configuration.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(fastJsonRedisSerializer)).entryTtl(Duration.ofDays(1));
        return configuration;
    }


    //元数据的redisTemplate，用于查询数据资产系统的元数据
    @Primary
    @Bean(name = "oneLettuceConnectionFactory")
    public LettuceConnectionFactory oneLettuceConnectionFactory() {
        LettuceClientConfiguration clientConfig =
                LettucePoolingClientConfiguration.builder().commandTimeout(Duration.ofMillis(timeout))
                        .poolConfig(onePoolConfig()).build();
        return new LettuceConnectionFactory(oneRedisConfig(), clientConfig);
    }

    @Primary
    @Bean(name = "redisTemplate")
    public RedisTemplate<Object, Object> oneRedisTemplate() {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        //序列化
        /**
         * value 序列化
         */
        FastJsonRedisSerializer fastJsonRedisSerializer = new FastJsonRedisSerializer(Object.class);
        // value值的序列化采用fastJsonRedisSerializer
        template.setValueSerializer(fastJsonRedisSerializer);
        template.setHashValueSerializer(fastJsonRedisSerializer);

        // 全局开启AutoType，不建议使用
        // ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
        // 建议使用这种方式，小范围指定白名单
        ParserConfig.getGlobalInstance().addAccept("com.demon.domain");
        ParserConfig.getGlobalInstance().addAccept("com.demon.modules.system.service.dto");
        ParserConfig.getGlobalInstance().addAccept("com.demon.modules.system.service.feign");
        ParserConfig.getGlobalInstance().addAccept("com.demon.modules.system.domain");
        ParserConfig.getGlobalInstance().addAccept("com.demon.modules.quartz.domain");
        ParserConfig.getGlobalInstance().addAccept("com.demon.modules.monitor.domain");
        ParserConfig.getGlobalInstance().addAccept("com.demon.modules.security.security");
        // key的序列化采用StringRedisSerializer
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setConnectionFactory(oneLettuceConnectionFactory());
        return template;
    }

    @Value("${spring.redis.host:127.0.0.1}")
    private String host;
    @Value("${spring.redis.port:6379}")
    private Integer port;
    @Value("${spring.redis.password:}")
    private String password;
    @Value("${spring.redis.database:0}")
    private Integer database;
    @Value("${spring.redis.timeout:10000}")
    private Long timeout;

    @Value("${spring.redis.lettuce.pool.max-active:8}")
    private Integer maxActive;
    @Value("${spring.redis.lettuce.pool.max-idle:8}")
    private Integer maxIdle;
    @Value("${spring.redis.lettuce.pool.max-wait:-1}")
    private Long maxWait;
    @Value("${spring.redis.lettuce.pool.min-idle:0}")
    private Integer minIdle;

    public GenericObjectPoolConfig onePoolConfig() {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(maxActive);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        config.setMaxWaitMillis(maxWait);
        return config;
    }

    public RedisStandaloneConfiguration oneRedisConfig() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPassword(RedisPassword.of(password));
        config.setPort(port);
        config.setDatabase(database);
        return config;
    }
}
