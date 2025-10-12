package org.shop.apiserver.infrastructure.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        String address = String.format("redis://%s:%d", redisHost, redisPort);

        config.useSingleServer()
                .setAddress(address)
                .setPassword(redisPassword.isEmpty() ? null : redisPassword)
                .setConnectionMinimumIdleSize(2)
                .setConnectionPoolSize(10)
                .setRetryAttempts(3)
                .setRetryInterval(1500);

        return Redisson.create(config);
    }
}
