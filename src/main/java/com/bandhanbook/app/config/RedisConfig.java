package com.bandhanbook.app.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
//@EnableCaching
public class RedisConfig {

  //  @Bean
    public ReactiveRedisTemplate<String, Object> redisTemplate(
            ReactiveRedisConnectionFactory factory
    ) {

        RedisSerializationContext<String, Object> context =
                RedisSerializationContext
                        .<String, Object>newSerializationContext(new StringRedisSerializer())
                        .value(new GenericJackson2JsonRedisSerializer())
                        .build();
        return new ReactiveRedisTemplate<>(factory, context);
    }
}