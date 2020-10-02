package de.romqu.schimmelhofapi.data.session

import com.fasterxml.jackson.databind.ObjectMapper
import de.romqu.schimmelhofapi.shared.Result
import org.springframework.stereotype.Repository
import redis.clients.jedis.Jedis
import java.util.*

@Repository
class SessionRepository(
    private val objectMapper: ObjectMapper,
    private val jedis: Jedis,
) {

    fun saveOrUpdate(sessionEntity: SessionEntity): SessionEntity {
        val sessionJson = objectMapper.writeValueAsString(sessionEntity)

        jedis.set(sessionEntity.uuid.toString(), sessionJson)

        return sessionEntity
    }

    fun getBy(uuid: UUID): Result<SessionDoesNotExistError, SessionEntity> {
        val sessionJson: String? = jedis.get(uuid.toString())

        return if (sessionJson != null) {
            val session = objectMapper.readValue(sessionJson, SessionEntity::class.java)

            Result.Success(session)
        } else {
            Result.Failure(SessionDoesNotExistError)
        }
    }

    object SessionDoesNotExistError
}


