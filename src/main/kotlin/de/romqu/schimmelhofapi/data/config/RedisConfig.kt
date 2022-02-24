package de.romqu.schimmelhofapi.data.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import redis.clients.jedis.Jedis

@Configuration
class RedisConfig {

    @Value("\${spring.redis.host}")
    lateinit var host: String

    @Value("\${spring.redis.port}")
    private var port = 0

    @Bean
    fun getRedisClient(): Jedis {
        return Jedis(host, port)
    }
}