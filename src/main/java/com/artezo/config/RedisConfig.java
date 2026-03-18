package com.artezo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.CommandLineRunner;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }

//    @Bean
//    public CommandLineRunner redisDebug(
//            @Value("${spring.data.redis.host}") String host,
//            @Value("${spring.data.redis.port}") int port,
//            @Value("${spring.data.redis.password}") String password) {
//        return args -> {
//            System.out.println("=== REDIS DEBUG ===");
//            System.out.println("Host: " + host);
//            System.out.println("Port: " + port);
//            System.out.println("Password empty: " + password.isEmpty());
//            System.out.println("===================");
//        };
//    }
}