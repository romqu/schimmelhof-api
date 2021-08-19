package de.romqu.schimmelhofapi.domain.ridinglesson

import de.romqu.schimmelhofapi.data.ridinglesson.RidingLessonDayEntity
import de.romqu.schimmelhofapi.data.session.SessionEntity
import de.romqu.schimmelhofapi.data.week.WeekRepository
import de.romqu.schimmelhofapi.shared.Result
import de.romqu.schimmelhofapi.shared.mapWithError
import org.springframework.stereotype.Service

@Service
class GetRidingLessonDaysService(
    val getRidingLessonDaysTask: GetRidingLessonDaysTask,
    val weekRepository: WeekRepository,
) {

    fun execute(session: SessionEntity): Result<Error, List<RidingLessonDayEntity>> =
        getLessons(session)

    private fun getLessons(
        session: SessionEntity
    ): Result<Error, List<RidingLessonDayEntity>> =
        getRidingLessonDaysTask.execute(
            weekRepository.getAll().take(2),
            session
        ).mapWithError({ it }) { _ ->
            Error
        }

    object Error

}