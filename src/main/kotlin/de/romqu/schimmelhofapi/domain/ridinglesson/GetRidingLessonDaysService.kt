package de.romqu.schimmelhofapi.domain.ridinglesson

import de.romqu.schimmelhofapi.data.ridinglesson.RidingLessonDayEntity
import de.romqu.schimmelhofapi.data.session.SessionEntity
import de.romqu.schimmelhofapi.data.session.SessionRepository
import de.romqu.schimmelhofapi.data.week.WeekRepository
import de.romqu.schimmelhofapi.shared.Result
import de.romqu.schimmelhofapi.shared.flatMap
import de.romqu.schimmelhofapi.shared.mapWithError
import org.springframework.stereotype.Service
import java.util.*

@Service
class GetRidingLessonDaysService(
    val getRidingLessonDaysTask: GetRidingLessonDaysTask,
    val sessionRepository: SessionRepository,
    val weekRepository: WeekRepository,
) {

    fun execute(sessionId: UUID): Result<Error, List<RidingLessonDayEntity>> =
        sessionRepository.getBy(sessionId)
            .getLessons()


    private fun Result<SessionRepository.SessionDoesNotExistError, SessionEntity>.getLessons(): Result<Error, List<RidingLessonDayEntity>> =
        flatMap { session ->
            getRidingLessonDaysTask.execute(
                weekRepository.getAll().take(2),
                session
            ).mapWithError({ it }) { error ->
                Error
            }
        }.mapWithError { error ->
            Error
        }

    object Error

}