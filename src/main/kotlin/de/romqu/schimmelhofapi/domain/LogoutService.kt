package de.romqu.schimmelhofapi.domain

import de.romqu.schimmelhofapi.data.UserRepository
import de.romqu.schimmelhofapi.data.ridinglesson.RidingLessonRepository
import de.romqu.schimmelhofapi.data.session.SessionEntity
import de.romqu.schimmelhofapi.data.session.SessionRepository
import de.romqu.schimmelhofapi.data.week.WeekRepository
import de.romqu.schimmelhofapi.shared.Result
import de.romqu.schimmelhofapi.shared.map
import de.romqu.schimmelhofapi.shared.mapWithError
import org.springframework.stereotype.Service

@Service
class LogoutService(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val weekRepository: WeekRepository,
) {
    fun execute(session: SessionEntity): Result<Error, Unit> {
        val week = weekRepository.getAll().first()
        val from = week.days.first()
        val to = week.days.last()

        return userRepository.logout(
            from = from,
            to = to,
            cmdWeek = RidingLessonRepository.CmdWeek.SHOW_WEEK,
            session = session
        ).map { sessionRepository.delete(session) }
            .mapWithError({ }) { Error.Network }
    }

    sealed class Error {
        object Network : Error()
    }
}