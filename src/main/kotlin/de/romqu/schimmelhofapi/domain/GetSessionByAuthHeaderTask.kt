package de.romqu.schimmelhofapi.domain

import de.romqu.schimmelhofapi.data.session.SessionEntity
import de.romqu.schimmelhofapi.data.session.SessionRepository
import de.romqu.schimmelhofapi.shared.Result
import de.romqu.schimmelhofapi.shared.mapError
import org.springframework.stereotype.Service
import java.util.*
import java.util.regex.Pattern

val AUTH_BEARER_PATTERN: Pattern =
    Pattern.compile("^Bearer *([^ ]+) *$", Pattern.CASE_INSENSITIVE)

@Service
class GetSessionByAuthHeaderTask(
    private val sessionRepository: SessionRepository,
) {


    fun execute(authHeader: String?): Result<Error, SessionEntity> {

        authHeader ?: return Result.Failure(Error.HeaderDoesNotExist)

        val authMatcher = AUTH_BEARER_PATTERN.matcher(authHeader)

        val sessionUuid = if (authMatcher.matches()) {
            val token = authMatcher.group(1)

            try {
                UUID.fromString(token)
            } catch (ex: Exception) {
                return Result.Failure(Error.InvalidTokenFormat)
            }
        } else return Result.Failure(Error.InvalidTokenFormat)


        return sessionRepository.getBy(sessionUuid)
            .mapError(Error.SessionDoesNotExist)

    }

    sealed class Error {
        object HeaderDoesNotExist : Error()
        object InvalidTokenFormat : Error()
        object SessionDoesNotExist : Error()
    }
}