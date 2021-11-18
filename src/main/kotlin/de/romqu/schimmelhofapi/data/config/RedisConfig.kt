package de.romqu.schimmelhofapi.data.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import redis.clients.jedis.Jedis

@Configuration
class RedisConfig {

    @Bean
    fun getRedisClient(): Jedis = Jedis()
}