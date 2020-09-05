package de.romqu.schimmelhofapi.data

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Repository
import redis.clients.jedis.Jedis
import java.util.*

@Repository
class SessionRepository(
    private val objectMapper: ObjectMapper,
    private val jedis: Jedis
) {

    fun saveOrUpdate(sessionEntity: SessionEntity): SessionEntity {
        val sessionJson = objectMapper.writeValueAsString(sessionEntity)

        jedis.set(sessionEntity.uuid.toString(), sessionJson)

        return sessionEntity
    }

    fun get(uuid: UUID): SessionEntity {
        val sessionJson = jedis.get(uuid.toString())

        return objectMapper.readValue(sessionJson, SessionEntity::class.java)
    }
}


data class SessionEntity(
    val uuid: UUID,
    val cookie: String,
    val cookieWeb: String,
    val viewState: String,
    val viewStateGenerator: String,
    val eventValidation: String,
)